// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyBLRnzgRJpkibrEwXUi2qzRYNeV-8nT-3w",
  authDomain: "baru-fe8a3.firebaseapp.com",
  projectId: "baru-fe8a3",
  storageBucket: "baru-fe8a3.firebasestorage.app",
  messagingSenderId: "882339697819",
  appId: "1:882339697819:web:0b596cece88ab322ba623e",
  measurementId: "G-TL3EPYC1MC"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);