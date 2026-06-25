# SpyApp Admin Dashboard

Open `index.html` in a browser or serve this folder with any static web server.

## Logins

- Normal users log in with the same Firebase Email/Password account used in the Android app.
- Super admin login:
  - Email: `asifrezan.office@gmail.com`
  - Password: `admin0011`

## Firebase Data Paths

- `Users/{uid}` stores profile and subscription status.
- `Messages/{uid}/{platform}/{messageId}` stores captured messages from Android.
- `PaymentRequests/{requestId}` stores manual bKash/Rocket/Nagad payment submissions.
- `Subscriptions/{uid}` stores the active subscription record.

If your Realtime Database URL is different from `https://notificationreader-9b068-default-rtdb.firebaseio.com`, update `databaseURL` in `app.js`.
