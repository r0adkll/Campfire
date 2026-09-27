#!/usr/bin/env python3
"""Build-time and runtime performance benchmarks for Campfire. See tools/benchmark/README.md."""
import argparse
import csv
import json
import shutil
import statistics
import subprocess
import sys
from pathlib import Path

TOOL_DIR = Path(__file__).resolve().parent
ROOT = TOOL_DIR.parents[1]
RESULTS_DIR = TOOL_DIR / ".results"
SCENARIOS = TOOL_DIR / "build.scenarios"

RUNTIME_TEST = "app.campfire.baselineprofile.DiStartupBenchmarks"
RUNTIME_OUTPUT = ROOT / "app/baselineprofile/build/outputs/connected_android_test_additional_output"
# Where the benchmark writes its report on the device, pulled directly if Gradle's copy fails.
DEVICE_OUTPUT = "/sdcard/Android/media/app.campfire.baselineprofile/additional_test_output"


def label_dir(kind, label):
    return RESULTS_DIR / kind / label


def cmd_build(args):
    out = label_dir("build", args.label)
    if out.exists():
        shutil.rmtree(out)
    cmd = [
        "gradle-profiler",
        "--benchmark",
        "--project-dir", str(ROOT),
        "--scenario-file", str(SCENARIOS),
        "--output-dir", str(out),
        "--gradle-user-home", str(RESULTS_DIR / "gradle-user-home"),
        *args.scenarios,
    ]
    print("+", " ".join(cmd), flush=True)
    subprocess.run(cmd, check=True)
    print_build(out)


def read_build(out):
    """Parse gradle-profiler's benchmark.csv into {scenario title: [measured ms, ...]}."""
    with open(out / "benchmark.csv", newline="") as f:
        rows = list(csv.reader(f))
    titles = next(r for r in rows if r and r[0] == "scenario")[1:]
    results = {t: [] for t in titles}
    for row in rows:
        if row and row[0].startswith("measured build"):
            for title, value in zip(titles, row[1:]):
                if value:
                    results[title].append(float(value))
    return results


def summarize(values):
    return {
        "median": statistics.median(values),
        "mean": statistics.fmean(values),
        "min": min(values),
        "stdev": statistics.stdev(values) if len(values) > 1 else 0.0,
    }


def print_build(out):
    print(f"\nBuild results ({out.name})")
    for title, values in read_build(out).items():
        s = summarize(values)
        print(f"  {title}: median {s['median'] / 1000:.1f}s (min {s['min'] / 1000:.1f}s, n={len(values)})")


def cmd_runtime(args):
    out = label_dir("runtime", args.label)
    if out.exists():
        shutil.rmtree(out)
    if RUNTIME_OUTPUT.exists():
        shutil.rmtree(RUNTIME_OUTPUT)
    subprocess.run(["adb", "shell", "rm", "-rf", DEVICE_OUTPUT], check=False)
    runner_args = {
        "class": RUNTIME_TEST,
        "iterations": str(args.iterations),
    }
    if args.emulator:
        runner_args["androidx.benchmark.suppressErrors"] = "EMULATOR"
    cmd = [
        str(ROOT / "gradlew"),
        ":app:baselineprofile:connectedBenchmarkReleaseAndroidTest",
        f"-Pcampfire.benchmark.flavor={args.flavor}",
        *[f"-Pandroid.testInstrumentationRunnerArguments.{k}={v}" for k, v in runner_args.items()],
    ]
    print("+", " ".join(cmd), flush=True)
    # A flaky USB connection can fail the task while pulling results after the test itself passed,
    # so a failure here isn't final until the report is found missing below.
    build = subprocess.run(cmd, cwd=ROOT)

    out.mkdir(parents=True)
    reports = list(RUNTIME_OUTPUT.rglob("*benchmarkData.json"))
    if reports:
        shutil.copy(reports[0], out / "benchmarkData.json")
    else:
        report = f"{DEVICE_OUTPUT}/app.campfire.baselineprofile-benchmarkData.json"
        pulled = subprocess.run(["adb", "pull", report, str(out / "benchmarkData.json")])
        if pulled.returncode != 0:
            sys.exit(f"Benchmark failed (exit {build.returncode}); no benchmarkData.json locally or on the device")
    for trace in RUNTIME_OUTPUT.rglob("*.perfetto-trace"):
        shutil.copy(trace, out / trace.name)
    print_runtime(out)


def read_runtime(out):
    """Parse a macrobenchmark report into {metric: [run values, ...]}."""
    data = json.loads((out / "benchmarkData.json").read_text())
    results = {}
    for benchmark in data["benchmarks"]:
        for name, metric in benchmark["metrics"].items():
            results[f"{benchmark['name']}.{name}"] = metric["runs"]
    return results


def print_runtime(out):
    print(f"\nRuntime results ({out.name})")
    for name, values in read_runtime(out).items():
        s = summarize(values)
        print(f"  {name}: median {s['median']:.2f} (min {s['min']:.2f}, n={len(values)})")


def compare(before, after, unit):
    print(f"| Metric | {before[0]} (median) | {after[0]} (median) | Δ | Δ% |")
    print("|---|---:|---:|---:|---:|")
    for name in before[1]:
        if name not in after[1]:
            continue
        b = statistics.median(before[1][name])
        a = statistics.median(after[1][name])
        delta = a - b
        pct = (delta / b * 100) if b else 0.0
        print(f"| {name} | {unit(b)} | {unit(a)} | {unit(delta, signed=True)} | {pct:+.1f}% |")


def cmd_compare(args):
    build_before, build_after = label_dir("build", args.before), label_dir("build", args.after)
    if (build_before / "benchmark.csv").exists() and (build_after / "benchmark.csv").exists():
        print("### Build time\n")
        compare(
            (args.before, read_build(build_before)),
            (args.after, read_build(build_after)),
            lambda v, signed=False: f"{v / 1000:+.1f}s" if signed else f"{v / 1000:.1f}s",
        )
        print()
    runtime_before, runtime_after = label_dir("runtime", args.before), label_dir("runtime", args.after)
    if (runtime_before / "benchmarkData.json").exists() and (runtime_after / "benchmarkData.json").exists():
        print("### Runtime\n")
        compare(
            (args.before, read_runtime(runtime_before)),
            (args.after, read_runtime(runtime_after)),
            lambda v, signed=False: f"{v:+.2f}" if signed else f"{v:.2f}",
        )


def main(argv):
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest="command", required=True)

    build = sub.add_parser("build", help="Profile build times with gradle-profiler")
    build.add_argument("--label", required=True, help="Name to archive results under, e.g. kotlin-inject")
    build.add_argument("scenarios", nargs="*", help="Scenario names from build.scenarios; default = its defaults")
    build.set_defaults(func=cmd_build)

    runtime = sub.add_parser("runtime", help="Run the DI startup macrobenchmark on a connected device")
    runtime.add_argument("--label", required=True, help="Name to archive results under, e.g. kotlin-inject")
    runtime.add_argument("--iterations", type=int, default=20)
    runtime.add_argument(
        "--flavor",
        default="foss",
        help="App flavor to benchmark (default foss: no Firebase, so no google-services.json needed)",
    )
    runtime.add_argument("--emulator", action="store_true", help="Allow running on an emulator (noisier)")
    runtime.set_defaults(func=cmd_runtime)

    comp = sub.add_parser("compare", help="Print a markdown comparison of two labels")
    comp.add_argument("before")
    comp.add_argument("after")
    comp.set_defaults(func=cmd_compare)

    args = p.parse_args(argv)
    args.func(args)


if __name__ == "__main__":
    main(sys.argv[1:])
