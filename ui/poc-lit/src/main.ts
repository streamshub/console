import { Router } from '@vaadin/router';
import { routes } from './router/routes';

// Initialize router
const outlet = document.getElementById('app');
if (outlet) {
  const router = new Router(outlet);
  router.setRoutes(routes);
}

// Log router initialization
console.log('StreamsHub Console initialized with Vaadin Router');

// Made with Bob
