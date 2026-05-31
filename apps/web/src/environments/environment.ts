export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  msalConfig: {
    auth: {
      clientId: '9c042f66-5b72-4dcf-9f65-7fd1bf196bb6',
      authority: 'https://trackmanager.b2clogin.com/trackmanager.onmicrosoft.com/B2C_1_susi',
      knownAuthorities: ['trackmanager.b2clogin.com'],
      redirectUri: 'http://localhost:4200',
      postLogoutRedirectUri: 'http://localhost:4200/login',
    },
    cache: {
      cacheLocation: 'localStorage',
      storeAuthStateInCookie: false,
    },
  },
  apiScopes: ['openid', 'profile'],
};
