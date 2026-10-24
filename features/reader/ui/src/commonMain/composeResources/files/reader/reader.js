// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

// Glue between the Campfire app and foliate-js. The app calls `window.campfire.*`; this page
// answers with JSON messages through the platform's script message handler.
//
// Reading positions are kept in the format the official Audiobookshelf readers use, so a position
// saved here opens in the same place there and vice versa:
//   EPUB:  an epub.js CFI. foliate-js and epub.js count text nodes differently after inline
//          elements, so CFIs are written and resolved with epub.js's own EpubCFI, vendored.
//   CBZ:   a 1-based page number.
//   other: foliate's own CFI (the official readers save nothing for MOBI/AZW3/FB2).

import './foliate/view.js'
import EpubCFI from './epubjs-cfi/epubcfi.js'

const post = message => {
  const json = JSON.stringify(message)
  if (globalThis.webkit?.messageHandlers?.campfire) {
    globalThis.webkit.messageHandlers.campfire.postMessage(json)
  } else if (globalThis.CampfireAndroid) {
    globalThis.CampfireAndroid.postMessage(json)
  } else {
    console.log('campfire', json)
  }
}

const describe = e => String(e?.stack ?? e?.message ?? e)
// Errors outside opening the book are reported, not fatal: some are harmless, like the browser's
// "ResizeObserver loop completed with undelivered notifications" when the window resizes
globalThis.addEventListener('error', e => post({ type: 'error', message: describe(e.error ?? e.message), fatal: false }))
globalThis.addEventListener('unhandledrejection', e => post({ type: 'error', message: describe(e.reason), fatal: false }))

let view
let format
// Whether the opening position, saved or not, has been laid out: moves before that are the book
// opening, not the reader turning pages
let settled = false

const isEpub = () => format === 'epub'
const isComic = () => format === 'cbz'

// epub.js builds its CFI base from the package step, the spine position and the itemref idref:
// `/6/4[chapter-1]`. foliate's section CFI holds the package step and position without the idref.
const epubjsBase = index => {
  const { book } = view
  const steps = book.sections[index].cfi.replace(/^epubcfi\(|\)$/g, '').split('/').filter(Boolean)
  const spineNodeIndex = parseInt(steps[0], 10) / 2 - 1
  const idref = book.resources.spine[index].idref
  return new EpubCFI().generateChapterComponent(spineNodeIndex, index, idref)
}

const currentIndex = detail => {
  const doc = detail.range?.startContainer?.ownerDocument
  const contents = view.renderer.getContents()
  return contents.find(c => c.doc === doc)?.index ?? detail.section?.current ?? contents[0]?.index
}

// Whether a text node is laid out at all: books hide some headings (an "Imprint" title, say) with
// CSS, and a position there has no box to show
const isRendered = node => {
  const element = node.parentElement
  if (!element) return true
  return element.checkVisibility?.() ?? element.getClientRects().length > 0
}

// The start of `range` moved onto its first visible, non-whitespace character. A visible range
// often starts on the whitespace between elements, or on hidden text, neither of which has a
// layout box: epub.js resolves a CFI there by its box, so it would open at the start of the
// section instead, and foliate would open on a blank page.
const visibleStart = range => {
  const start = range.cloneRange()
  start.collapse(true)
  const { startContainer, startOffset } = range
  const doc = startContainer.ownerDocument
  const walker = doc.createTreeWalker(range.commonAncestorContainer, NodeFilter.SHOW_TEXT)
  if (startContainer.nodeType === Node.TEXT_NODE) {
    const at = startContainer.nodeValue.slice(startOffset).search(/\S/)
    if (at >= 0 && isRendered(startContainer)) {
      start.setStart(startContainer, startOffset + at)
      return start
    }
    walker.currentNode = startContainer
  } else {
    const child = startContainer.childNodes[startOffset]
    if (child?.nodeType === Node.TEXT_NODE && /\S/.test(child.nodeValue) && isRendered(child)) {
      start.setStart(child, child.nodeValue.search(/\S/))
      return start
    }
    if (child) walker.currentNode = child
  }
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    if (range.comparePoint(node, node.length) < 0) continue
    if (range.comparePoint(node, 0) > 0) break
    const at = node.nodeValue.search(/\S/)
    if (at >= 0 && isRendered(node)) {
      start.setStart(node, at)
      return start
    }
  }
  return start
}

// A saved position on hidden text (which another reader may have saved) moved onto the visible
// text after it, so the book doesn't open on a blank page
const visibleAnchor = range => {
  if (!range || range.getClientRects().length > 0) return range
  const doc = range.startContainer.ownerDocument
  const rest = doc.createRange()
  rest.setStart(range.startContainer, range.startOffset)
  rest.setEnd(doc.body, doc.body.childNodes.length)
  return visibleStart(rest)
}

const toAbsLocation = detail => {
  const index = currentIndex(detail)
  if (isEpub() && detail.range && index != null) {
    const start = visibleStart(detail.range)
    return { location: new EpubCFI(start, epubjsBase(index)).toString(), progress: detail.fraction }
  }
  if (isComic() && index != null) {
    const total = view.book.sections.length
    return { location: String(index + 1), progress: total ? index / total : 0 }
  }
  return { location: detail.cfi, progress: detail.fraction }
}

// Show a position in the Audiobookshelf format; false when it can't be read
const goToAbsLocation = async location => {
  if (location && isEpub() && location.startsWith('epubcfi')) {
    const { index } = view.book.resolveCFI(location)
    if (index >= 0) {
      const cfi = new EpubCFI(location)
      await view.renderer.goTo({ index, anchor: doc => visibleAnchor(cfi.toRange(doc)) })
      return true
    }
  }
  if (location && isComic() && /^\d+$/.test(location)) {
    await view.renderer.goTo({ index: Math.max(0, parseInt(location, 10) - 1) })
    return true
  }
  if (location && !isEpub() && !isComic() && location.startsWith('epubcfi')) {
    await view.goTo(location)
    return true
  }
  return false
}

const restore = async location => {
  if (location && !isEpub() && !isComic() && location.startsWith('epubcfi')) {
    await view.init({ lastLocation: location })
    return true
  }
  if (await goToAbsLocation(location)) return true
  await view.init({ showTextStart: false })
  return false
}

// Where in the book an item of the table of contents starts, 0..1, to mark chapters on the
// scrubber; null when its target can't be found
const fractionOf = href => {
  try {
    const { index } = view.book.resolveHref(href) ?? {}
    return index >= 0 ? (view.getSectionFractions()[index] ?? null) : null
  } catch {
    return null
  }
}

// Each item of the table of contents to the one it's nested in, to name the part a chapter is in
// ("A Scandal in Bohemia" over "I")
const tocParents = new WeakMap()

const flattenToc = (items, depth = 0, out = [], parent = null) => {
  for (const item of items ?? []) {
    const href = item.href ?? ''
    if (parent) tocParents.set(item, parent)
    out.push({ label: (item.label ?? '').trim(), href, depth, fraction: fractionOf(href) })
    flattenToc(item.subitems, depth + 1, out, item)
  }
  return out
}

const chapterTitle = tocItem => tocItem?.label?.trim() || null

// Only alongside the chapter's own title
const partTitle = tocItem => (chapterTitle(tocItem) && tocParents.get(tocItem)?.label?.trim()) || null

// Pages after this one in the current chapter, for the reflowable layout. The paginator counts a
// blank page before and after each section's text.
const pagesLeftInSection = () => {
  if (view.isFixedLayout) return null
  const { page, pages } = view.renderer
  return Number.isFinite(page) && Number.isFinite(pages) ? Math.max(0, pages - 2 - page) : null
}

const isAtEnd = detail => {
  const last = view.book.sections.length - 1
  if (view.isFixedLayout) return currentIndex(detail) === last
  return currentIndex(detail) === last && pagesLeftInSection() === 0
}

// The fonts bundled with the reader, served beside this page
const FONTS = {
  'Literata': 'literata',
  'Atkinson Hyperlegible Next': 'atkinson-hyperlegible-next',
  'OpenDyslexic': 'opendyslexic',
}

const fontFaces = Object.entries(FONTS).flatMap(([family, file]) =>
  [['400', 'normal'], ['400', 'italic'], ['700', 'normal'], ['700', 'italic']].map(([weight, style]) => `
    @font-face {
      font-family: '${family}';
      font-weight: ${weight};
      font-style: ${style};
      src: url('${new URL(`fonts/${file}-latin-${weight}-${style}.woff2`, document.baseURI)}') format('woff2');
    }`)).join('\n')

// Mirrors the foliate-js reader: the book's own rules still win where they're more specific (a
// centered heading), but its paragraphs take the reader's spacing, alignment and hyphenation
const css = style => `
  ${fontFaces}
  html {
    color: ${style.foreground};
    background: ${style.background};
    font-size: ${style.fontScale}%;
    ${style.fontFamily ? `font-family: ${style.fontFamily} !important;` : ''}
  }
  body {
    color: inherit !important;
    background: transparent !important;
  }
  ${style.fontFamily ? `
  body, body * {
    font-family: ${style.fontFamily} !important;
  }
  code, pre, kbd, samp, code *, pre * {
    font-family: monospace !important;
  }` : ''}
  p, li, blockquote, dd {
    line-height: ${style.lineHeight};
    text-align: ${style.justify ? 'justify' : 'start'};
    -webkit-hyphens: ${style.hyphenate ? 'auto' : 'manual'};
    hyphens: ${style.hyphenate ? 'auto' : 'manual'};
  }
  [align="left"] { text-align: left; }
  [align="right"] { text-align: right; }
  [align="center"] { text-align: center; }
  pre { white-space: pre-wrap !important; }
  a:any-link {
    color: ${style.link};
  }
`

const applyStyle = style => {
  document.body.style.background = style.background
  view.renderer.setStyles?.(css(style))
  const { renderer } = view
  renderer.setAttribute('flow', style.scrolled ? 'scrolled' : 'paginated')
  renderer.setAttribute('margin', '32px')
  renderer.setAttribute('gap', `${style.gap}%`)
  renderer.setAttribute('max-inline-size', '720px')
  renderer.setAttribute('max-column-count', String(style.columns))
  renderer.setAttribute('max-column-count-portrait', String(style.portraitColumns))
  renderer.setAttribute('animated', '')
}

const handleTaps = doc => {
  // The paginator turns reflowable pages on swipes itself; the fixed-layout renderer (comics,
  // fixed-layout EPUB) has no touch handling
  if (view.isFixedLayout) {
    let start
    doc.addEventListener('touchstart', e => {
      start = e.touches.length === 1 ? e.touches[0] : null
    }, { passive: true })
    doc.addEventListener('touchend', e => {
      const end = e.changedTouches[0]
      if (!start || !end) return
      const dx = end.screenX - start.screenX
      const dy = end.screenY - start.screenY
      start = null
      if (Math.abs(dx) < 40 || Math.abs(dx) < Math.abs(dy)) return
      if (dx < 0) view.goRight()
      else view.goLeft()
    })
  }

  doc.addEventListener('click', e => {
    // foliate-js handles link clicks first and marks them handled
    if (e.defaultPrevented) return
    if (!doc.getSelection()?.isCollapsed) return
    const frame = doc.defaultView?.frameElement
    const x = e.clientX + (frame?.getBoundingClientRect().left ?? 0)
    const width = globalThis.innerWidth
    if (x < width * 0.25) view.goLeft()
    else if (x > width * 0.75) view.goRight()
    else post({ type: 'tap' })
  })
}

globalThis.campfire = {
  async open({ url, format: bookFormat, location, style }) {
    try {
      format = bookFormat
      view = document.createElement('foliate-view')
      document.body.append(view)
      view.addEventListener('load', e => handleTaps(e.detail.doc))
      await view.open(url)
      // The renderer's event carries why the page moved; the view's handler, which runs first,
      // has already worked out the position into `view.lastLocation`
      view.renderer.addEventListener('relocate', e => {
        const detail = view.lastLocation
        if (!detail) return
        const { location, progress } = toAbsLocation(detail)
        post({
          type: 'relocate',
          location,
          progress,
          part: partTitle(detail.tocItem),
          chapter: chapterTitle(detail.tocItem),
          chapterHref: detail.tocItem?.href ?? null,
          pagesLeft: pagesLeftInSection(),
          atEnd: isAtEnd(detail),
          // 'anchor' lays the same position out again (a resize, late images); anything else
          // after the restore is the reader moving
          save: settled && e.detail.reason !== 'anchor' && e.detail.reason !== 'selection',
        })
      })
      applyStyle(style)

      const { book } = view
      post({
        type: 'opened',
        title: book.metadata?.title ?? null,
        toc: flattenToc(book.toc),
      })
      const restored = await restore(location)
      settled = true
      post({ type: 'restored', restored })
    } catch (e) {
      post({ type: 'error', message: describe(e), fatal: true })
    }
  },
  next: () => view?.goRight(),
  prev: () => view?.goLeft(),
  goTo: href => view?.goTo(href),
  goToFraction: fraction => view?.goToFraction(fraction),
  goToLocation: location => view && goToAbsLocation(location),
  setStyle: style => view && applyStyle(style),
}

post({ type: 'ready' })
