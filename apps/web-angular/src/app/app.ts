import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
  styles: `:host { display: block; height: 100%; }`,
})
export class AppComponent implements OnInit {
  private readonly swUpdate = inject(SwUpdate, { optional: true });

  ngOnInit(): void {
    if (!this.swUpdate?.isEnabled) {
      return;
    }
    this.swUpdate.versionUpdates
      .pipe(filter((event): event is VersionReadyEvent => event.type === 'VERSION_READY'))
      .subscribe(() => {
        this.swUpdate?.activateUpdate().then(() => {
          window.location.reload();
        });
      });
    this.swUpdate.checkForUpdate().catch(() => undefined);
  }
}
