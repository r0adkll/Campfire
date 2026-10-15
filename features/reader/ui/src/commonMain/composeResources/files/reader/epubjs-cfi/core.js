// Minimal subset of epubjs/src/utils/core.js needed by epubcfi.js (copied verbatim)
export function isNumber(n) { return !isNaN(parseFloat(n)) && isFinite(n); }
export function extend(target) {
	var sources = [].slice.call(arguments, 1);
	sources.forEach(function (source) {
		if(!source) return;
		Object.getOwnPropertyNames(source).forEach(function(propName) {
			Object.defineProperty(target, propName, Object.getOwnPropertyDescriptor(source, propName));
		});
	});
	return target;
}
export function type(obj){ return Object.prototype.toString.call(obj).slice(8, -1); }
export function findChildren(el) {
	var result = [];
	var childNodes = el.childNodes;
	for (var i = 0; i < childNodes.length; i++) { let node = childNodes[i]; if (node.nodeType === 1) result.push(node); }
	return result;
}
// Only used when document.createRange is missing (never in a browser/WebView)
export class RangeObject { constructor() { throw new Error("RangeObject polyfill not vendored"); } }
