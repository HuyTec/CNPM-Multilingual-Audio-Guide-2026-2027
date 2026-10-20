import { expect, test } from '@playwright/test';

test('actual Google SDK and tiles with configured local key; shared markers open real text', async ({ page }, info) => {
  test.skip(process.env.LIVE_GOOGLE_MAPS !== '1', 'Requires configured Google key; makes actual Google map requests');
  const errors: string[] = [];
  const safeLogs:string[]=[];
  page.on('pageerror',error=>safeLogs.push(error.message.replace(/AIza[\w-]+/g,'[redacted]').replace(/https?:\/\/\S+/g,'[URL]').slice(0,500)));
  const failedHosts: string[] = [];
  await page.addInitScript(()=>{Object.assign(window,{testGoogleAuthFailed:false});window.addEventListener('google-maps-auth-failure',()=>Object.assign(window,{testGoogleAuthFailed:true}));});
  page.on('response',async response=>{
    const url=new URL(response.url());
    if(url.hostname.includes('google') && url.pathname.includes('AuthenticationService')){
      const body=(await response.text().catch(()=>''));safeLogs.push(body.replace(/AIza[\w-]+/g,'[redacted]').replace(/https?:\/\/\S+/g,'[URL]').slice(0,1000));
    }
  });
  page.on('requestfailed', request => { const url = new URL(request.url()); if (url.hostname.includes('google')) failedHosts.push(`${url.hostname}: ${request.failure()?.errorText}`); });
  // Record only error codes. Never persist SDK URLs, credentials, or raw console messages.
  page.on('console', message => {
    if(message.text().includes('Google')||message.text().includes('TravelVoice map'))safeLogs.push(message.text().replace(/AIza[\w-]+/g,'[redacted]').replace(/https?:\/\/\S+/g,'[URL]').slice(0,500));
    if(message.text().startsWith('TravelVoice map setup:'))errors.push(message.text());
    const code = message.text().match(/\b[A-Za-z]+(?:MapError|MapWarning)\b/)?.[0];
    if (code) errors.push(code);
    else if(message.type()==='error' && message.text().includes('Google Maps'))errors.push(message.text().replace(/AIza[\w-]+/g,'[redacted]').replace(/https?:\/\/\S+/g,'[URL]').slice(0,300));
  });
  await page.goto('/?view=map');
  await expect(page.locator('.tour-map')).toBeVisible();
  try {
    await expect(page.getByText('Bản đồ đã sẵn sàng.', { exact: true })).toBeVisible({ timeout: 30_000 });
  } catch (error) {
    console.log({ errors, failedHosts, safeLogs, authRejected:await page.evaluate(()=>(window as unknown as {testGoogleAuthFailed:boolean}).testGoogleAuthFailed), sdkLoaded: await page.evaluate(() => !!window.google?.maps?.Map), markers: await page.locator('.map-marker').count() });
    throw error;
  }
  await expect(page.locator('.map-marker')).toHaveCount(4);
  await expect.poll(() => page.evaluate(() => !!window.google?.maps?.Map)).toBe(true);
  expect(errors).toEqual([]);
  await page.locator('.tour-map').screenshot({ path: info.outputPath('google-map.png') });
  await page.getByRole('link', { name: 'Mở Dinh Độc Lập', exact: true }).click();
  await expect(page.locator('.script-text')).toBeVisible();
  await expect(page).toHaveURL(/000101\?language=VI&scriptType=FULL$/);
});
