import { test, chromium } from '@playwright/test';

// Helper function to create a delay
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

test('Simulate 5 concurrent users on Angular local server', async () => {
  const browser = await chromium.launch(); // Headless by default

  const userActions = Array.from({ length: 5 }).map(async (_, index) => {
    const userId = index + 1;
    
    // Stagger the startup: User 2 waits 500ms, User 3 waits 1000ms, etc.
    await delay(index * 500); 

    const context = await browser.newContext();
    const page = await context.newPage();

    console.log(`User ${userId}: Navigating to Angular app...`);
    await page.goto('http://localhost:4200');

    // Wait 5 seconds to hold the concurrent load
    await page.waitForTimeout(5000); 

    console.log(`User ${userId}: Finished action.`);
    await context.close();
  });

  await Promise.all(userActions);
  await browser.close();
});