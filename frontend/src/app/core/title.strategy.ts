import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/** "Page · Bloomé" titles for every route (announced by screen readers on navigation). */
@Injectable()
export class BloomeTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const page = this.buildTitle(snapshot);
    this.title.setTitle(page ? `${page} | Bloomé` : 'Bloomé, fleuriste en ligne à Rabat');
  }
}
