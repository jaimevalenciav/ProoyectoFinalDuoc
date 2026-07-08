export const environment = {
  production: true,
  apiUrl: '/api/v1',   // nginx proxia /api → bff-web (ACA interno)
  msalConfig: {
    auth: {
      clientId: '9c042f66-5b72-4dcf-9f65-7fd1bf196bb6',
      authority: 'https://trackmanager.b2clogin.com/trackmanager.onmicrosoft.com/B2C_1_susi',
      knownAuthorities: ['trackmanager.b2clogin.com'],
      redirectUri: 'https://web.greenwater-e373a369.eastus2.azurecontainerapps.io',
      postLogoutRedirectUri: 'https://web.greenwater-e373a369.eastus2.azurecontainerapps.io/login',
    },
    cache: {
      cacheLocation: 'localStorage',
      storeAuthStateInCookie: false,
    },
  },
  apiScopes: ['https://trackmanager.onmicrosoft.com/9c042f66-5b72-4dcf-9f65-7fd1bf196bb6/read'],
};
