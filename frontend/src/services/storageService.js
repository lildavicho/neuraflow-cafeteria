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

export const deleteImage = async (imageUrl) => {
  try {
    if (!imageUrl) return;

    const imageRef = ref(storage, imageUrl);
    await deleteObject(imageRef);
  } catch (error) {
    console.error('Error deleting image:', error);
  }
};

export const uploadAvatar = async (file) => {
  return uploadImage(file, 'avatars');
};
