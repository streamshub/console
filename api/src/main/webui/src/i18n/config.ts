/**
 * i18n Configuration using react-i18next
 */

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

// Import English messages
// Note: We'll copy the messages from the existing Next.js app
import enMessages from './messages/en.json';

i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: {
        translation: enMessages
      }
    },
    lng: 'en',
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false // React already escapes values
    },
    react: {
      useSuspense: false
    }
  });

export default i18n;