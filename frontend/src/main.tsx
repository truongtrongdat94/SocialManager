import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import "@/assets/styles/index.css";
import App from "./App.tsx"

if (window.location.hash === "#_=_" || window.location.hash === "#_") {
    const cleanUrl =
        window.location.pathname + window.location.search;

    window.history.replaceState(null, document.title, cleanUrl);
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
