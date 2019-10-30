import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { translationChunksConfig, translations } from '@spartacus/assets';
import { OccConfig } from '@spartacus/core';
import { B2cStorefrontModule } from '@spartacus/storefront';
import { environment } from '../environments/environment';
import { AppComponent } from './app.component';

const occConfig: OccConfig = { backend: { occ: {} } };

// only provide the `occ.baseUrl` key if it is explicitly configured, otherwise the value of
// <meta name="occ-backend-base-url" > is ignored.
// This in turn breaks the deployment in CCv2
if (environment.occBaseUrl) {
  occConfig.backend.occ.baseUrl = environment.occBaseUrl;
}

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    B2cStorefrontModule.withConfig({
      backend: occConfig.backend,
      authentication: {
        client_id: 'mobile_android',
        client_secret: 'secret',
      },
      context: {
        baseSite: ['electronics-spa'],
      },
      i18n: {
        resources: translations,
        chunks: translationChunksConfig,
        fallbackLang: 'en',
      },
      features: {
        level: '1.2',
      },
      personalization: {
        enabled: true,
      },
    }),
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
