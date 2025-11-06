import { ref, uploadBytes, getDownloadURL, deleteObject } from 'firebase/storage';
import { storage } from './firebase';

export const uploadImage = async (file, folder = 'productos') => {
  try {
    if (!file) throw new Error('No file provided');

    const timestamp = Date.now();
    const fileName = `${folder}/${timestamp}_${file.name}`;
    const storageRef = ref(storage, fileName);

    const snapshot = await uploadBytes(storageRef, file);
    const downloadURL = await getDownloadURL(snapshot.ref);

    return downloadURL;
  } catch (error) {
    console.error('Error uploading image:', error);
    throw new Error('Error al subir la imagen');
  }
};

const extractPathFromUrl = (downloadURL) => {
  try {
    const url = new URL(downloadURL);
    const bucket = storage.app.options.storageBucket;
    const apiPrefix = `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/`;

    if (downloadURL.startsWith(apiPrefix)) {
      return decodeURIComponent(downloadURL.slice(apiPrefix.length).split('?')[0]);
    }

    if (url.protocol === 'gs:') {
      return decodeURIComponent(url.pathname.slice(1));
    }
  } catch {
    // Not a valid URL, assume it is already a storage path
  }
  return downloadURL;
};

export const deleteImage = async (imageUrl) => {
  try {
    if (!imageUrl) return;

    const path = extractPathFromUrl(imageUrl);
    const imageRef = ref(storage, path);
    await deleteObject(imageRef);
  } catch (error) {
    console.error('Error deleting image:', error);
  }
};

export const uploadAvatar = async (file) => {
  return uploadImage(file, 'avatars');
};
