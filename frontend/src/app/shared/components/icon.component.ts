import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { inject } from '@angular/core';

/** Small inline icon set (stroke icons, 24px grid). Decorative unless a label is provided. */
const ICONS: Record<string, string> = {
  bag: '<path d="M5 8h14l-1.2 11.2a2 2 0 0 1-2 1.8H8.2a2 2 0 0 1-2-1.8L5 8Z"/><path d="M9 10V6.5a3 3 0 0 1 6 0V10"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4.5 20.5c1.4-3.6 4.2-5.5 7.5-5.5s6.1 1.9 7.5 5.5"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.2-4.2"/>',
  heart:
    '<path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7a4.3 4.3 0 0 1 7.5 2.8C19.5 15.4 12 20 12 20Z"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h10"/>',
  close: '<path d="M6 6l12 12M18 6 6 18"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  minus: '<path d="M5 12h14"/>',
  truck:
    '<path d="M3 6h11v10H3zM14 9h4l3 3v4h-7"/><circle cx="7" cy="17.5" r="1.8"/><circle cx="17.5" cy="17.5" r="1.8"/>',
  leaf: '<path d="M5 19c0-8 5-13 14-14 0 9-5 14-13 14"/><path d="M5 19 13 11"/>',
  gift: '<rect x="4" y="9" width="16" height="11" rx="1.5"/><path d="M3 9h18M12 9v11M12 9S10.5 4 8 4.6C6 5 6.6 9 12 9Zm0 0s1.5-5 4-4.4c2 .4 1.4 4.4-4 4.4Z"/>',
  check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
  trash:
    '<path d="M5 7h14M10 11v6M14 11v6M7 7l1 12a2 2 0 0 0 2 1.8h4a2 2 0 0 0 2-1.8L17 7M9.5 7V4.8h5V7"/>',
  star: '<path d="m12 4 2.4 4.9 5.4.8-3.9 3.8.9 5.4L12 16.4 7.2 18.9l.9-5.4-3.9-3.8 5.4-.8L12 4Z"/>',
  chevronLeft: '<path d="m15 5-7 7 7 7"/>',
  chevronRight: '<path d="m9 5 7 7-7 7"/>',
  chevronDown: '<path d="m6 9 6 6 6-6"/>',
  filter: '<path d="M4 6h16M7 12h10M10 18h4"/>',
  phone:
    '<path d="M6.5 3.5h3l1.5 4-2 1.3a10 10 0 0 0 6.2 6.2L16.5 13l4 1.5v3a2 2 0 0 1-2 2A16 16 0 0 1 4.5 5.5a2 2 0 0 1 2-2Z"/>',
  mail: '<rect x="3.5" y="5.5" width="17" height="13" rx="2"/><path d="m4 7 8 6 8-6"/>',
  pin: '<path d="M12 21s-6.5-6-6.5-11a6.5 6.5 0 0 1 13 0c0 5-6.5 11-6.5 11Z"/><circle cx="12" cy="10" r="2.3"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  instagram:
    '<rect x="4" y="4" width="16" height="16" rx="4.5"/><circle cx="12" cy="12" r="3.6"/><circle cx="17" cy="7" r=".8" fill="currentColor"/>',
  facebook:
    '<path d="M14.5 8.5H17V5h-2.5C12 5 11 6.6 11 9v2H8.5v3.5H11V21h3.5v-6.5H17l.5-3.5h-3v-1.6c0-.6.4-.9 1-.9Z"/>',
  whatsapp:
    '<path d="M4.5 19.5 5.6 16A8 8 0 1 1 8.4 18.6Z"/><path d="M9.3 8.8c.2 2.3 2.4 5 5.3 5.8l1.2-1.3-1.7-1-1 .6c-.9-.4-1.7-1.2-2-2l.6-1-1-1.7Z"/>',
  edit: '<path d="M4.5 19.5h4l10-10a2.8 2.8 0 0 0-4-4l-10 10v4Z"/><path d="m13 7 4 4"/>',
  box: '<path d="M4 8.5 12 4l8 4.5v7L12 20l-8-4.5v-7Z"/><path d="m4 8.5 8 4.5 8-4.5M12 13v7"/>',
  chart: '<path d="M4 20h16M7 16v-5M12 16V7M17 16v-8"/>',
  users:
    '<circle cx="9" cy="8.5" r="3.5"/><path d="M3 19.5c1-3 3.3-4.5 6-4.5s5 1.5 6 4.5"/><path d="M15.5 5.3a3.5 3.5 0 0 1 0 6.4M17.5 15.2c1.6.6 2.8 2 3.5 4.3"/>',
  layers:
    '<path d="m12 4 8.5 4.5L12 13 3.5 8.5 12 4Z"/><path d="m3.5 12.5 8.5 4.5 8.5-4.5M3.5 16.5 12 21l8.5-4.5"/>',
  tag: '<path d="M3.5 12.5v-8h8l9 9-8 8-9-9Z"/><circle cx="8" cy="8" r="1.5"/>',
  logout: '<path d="M10 5H5.5v14H10M14.5 8l4 4-4 4M18.5 12H9"/>',
  arrowLeft: '<path d="M19 12H5M11 6l-6 6 6 6"/>',
  alert: '<path d="M12 4 2.8 19.5h18.4L12 4Z"/><path d="M12 10v4.5M12 17.2v.3"/>',
  sparkle: '<path d="M12 3.5 13.8 10l6.7 2-6.7 2L12 20.5 10.2 14l-6.7-2 6.7-2L12 3.5Z"/>',
};

@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<svg
    viewBox="0 0 24 24"
    [attr.width]="size()"
    [attr.height]="size()"
    fill="none"
    stroke="currentColor"
    [attr.stroke-width]="stroke()"
    stroke-linecap="round"
    stroke-linejoin="round"
    [attr.aria-hidden]="label() ? null : 'true'"
    [attr.role]="label() ? 'img' : null"
    [attr.aria-label]="label()"
    [innerHTML]="svg()"
  ></svg>`,
  styles: `
    :host {
      display: inline-flex;
      line-height: 0;
    }
    svg {
      display: block;
    }
  `,
})
export class IconComponent {
  private readonly sanitizer = inject(DomSanitizer);
  readonly name = input.required<string>();
  readonly size = input(22);
  readonly stroke = input(1.6);
  readonly label = input<string | null>(null);
  // Icon markup is a static, trusted constant defined in this file.
  protected readonly svg = computed<SafeHtml>(() =>
    this.sanitizer.bypassSecurityTrustHtml(ICONS[this.name()] ?? ''),
  );
}
