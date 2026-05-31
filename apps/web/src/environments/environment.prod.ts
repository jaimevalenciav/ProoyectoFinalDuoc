export const environment = {
  production: true,
  apiUrl: '${API_URL}',
  msalConfig: {
    auth: {
      clientId: '${AZURE_CLIENT_ID}',
      authority: 'https://${AZURE_TENANT}.b2clogin.com/${AZURE_TENANT}.onmicrosoft.com/B2C_1_signin',
      knownAuthorities: ['${AZURE_TENANT}.b2clogin.com'],
      redirectUri: '${APP_URL}',
      postLogoutRedirectUri: '${APP_URL}/login',
    },
    cache: {
      cacheLocation: 'localStorage',
      storeAuthStateInCookie: false,
    },
  },
  apiScopes: ['https://${AZURE_TENANT}.onmicrosoft.com/api/read'],
};
