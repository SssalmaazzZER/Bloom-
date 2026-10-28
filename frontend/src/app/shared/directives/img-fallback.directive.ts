import { Directive, ElementRef, HostListener, inject } from '@angular/core';

/**
 * Graceful image fallback: if a photo fails to load, the <img> is replaced by a soft
 * botanical illustration (inline SVG) instead of a broken-image icon.
 */
const FALLBACK =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 500">
<rect width="400" height="500" fill="#F6E5E5"/>
<g fill="none" stroke="#C77983" stroke-width="3" stroke-linecap="round">
<path d="M200 430 C200 340 196 280 200 210"/>
<path d="M200 330 C170 310 150 300 130 300 C150 320 170 330 200 335"/>
<path d="M200 290 C230 272 252 266 272 268 C252 286 228 294 200 296"/>
</g>
<g fill="#C77983" fill-opacity=".55">
<ellipse cx="200" cy="168" rx="22" ry="42"/>
<ellipse cx="200" cy="168" rx="22" ry="42" transform="rotate(60 200 200)"/>
<ellipse cx="200" cy="168" rx="22" ry="42" transform="rotate(-60 200 200)"/>
</g>
<circle cx="200" cy="198" r="12" fill="#663B45"/>
</svg>`);

@Directive({ selector: 'img[appImgFallback]' })
export class ImgFallbackDirective {
  private readonly el = inject<ElementRef<HTMLImageElement>>(ElementRef);
  private applied = false;

  @HostListener('error')
  onError(): void {
    if (this.applied) return;
    this.applied = true;
    this.el.nativeElement.src = FALLBACK;
    this.el.nativeElement.removeAttribute('srcset');
    this.el.nativeElement.classList.add('img-fallback');
  }
}
