export default defineNuxtConfig({
  compatibilityDate: "2025-05-15",
  ssr: false,
  experimental: {
    asyncEntry: false,
    appManifest: false
  },
  devtools: {
    enabled: process.env.NUXT_DEVTOOLS === "true"
  },
  devServer: {
    host: process.env.NUXT_HOST || "0.0.0.0",
    port: Number.parseInt(process.env.NUXT_PORT || "3000", 10)
  },
  css: ["~/assets/css/main.css"],
  app: {
    head: {
      title: "Personal Finance Manager",
      meta: [
        {
          name: "viewport",
          content: "width=device-width, initial-scale=1"
        },
        {
          name: "description",
          content: "Nuxt frontend for the Personal Finance Manager API."
        }
      ],
      link: [
        { rel: "preconnect", href: "https://fonts.googleapis.com" },
        { rel: "preconnect", href: "https://fonts.gstatic.com", crossorigin: "" },
        {
          rel: "stylesheet",
          href: "https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,600;9..144,700&family=Manrope:wght@400;500;600;700;800&display=swap"
        }
      ]
    }
  },
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || "http://localhost:8080"
    }
  }
});
