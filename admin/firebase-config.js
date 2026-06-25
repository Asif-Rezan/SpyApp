import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.4/firebase-app.js";

const firebaseConfig = {
  apiKey: "AIzaSyBx68aOh915ljQXlZcdV-_QAuSOtNMgs1E",
  authDomain: "notificationreader-9b068.firebaseapp.com",
  databaseURL: "https://notificationreader-9b068-default-rtdb.firebaseio.com",
  projectId: "notificationreader-9b068",
  storageBucket: "notificationreader-9b068.firebasestorage.app",
  messagingSenderId: "48154620821",
  appId: "1:48154620821:web:43f193e2b12b2a4c49fc9c",
  measurementId: "G-WYNBS72EM0"
};

export const app = initializeApp(firebaseConfig);
export const analytics = getAnalytics(app);