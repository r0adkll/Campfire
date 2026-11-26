# Campfire

An unofficial native client for Audiobookshelf servers on Android, iOS and Desktop.

## Language

### Builds

**Build commit**:
The git commit a build was made from, identified by its short SHA, commit time and subject line. It is the commit's own time, never when the build ran.
_Avoid_: Build stamp, build time

**Build info**:
What a person sees and copies to identify their install: the version name and code with the build commit's short SHA, plus the build commit's time and subject on Alpha builds.
_Avoid_: Version info, about info, version string

**Alpha build**:
A build of the Alpha flavor, made from `main` or a pull request and distributed to testers. Only Android has Alpha builds today.
_Avoid_: Dev build, PR build, nightly
