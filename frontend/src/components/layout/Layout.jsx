import { Outlet } from 'react-router-dom';
import { useState, useLayoutEffect, useRef } from 'react';
import gsap from 'gsap';
import Sidebar from './Sidebar';
import Navbar from './Navbar';

const Layout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const contentRef = useRef(null);

  useLayoutEffect(() => {
    const ctx = gsap.context(() => {
      if (contentRef.current) {
        gsap.fromTo(
          contentRef.current,
          { opacity: 0, y: 20 },
          { opacity: 1, y: 0, duration: 0.3, ease: 'power2.out' }
        );
      }
    });
    return () => ctx.revert();
  }, []);

  return (
    <div className="flex h-screen bg-white dark:bg-gray-900 overflow-hidden">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      
      <div className="flex-1 flex flex-col overflow-hidden">
        <Navbar onMenuClick={() => setSidebarOpen(true)} />
        
        <main ref={contentRef} className="flex-1 overflow-y-auto bg-white dark:bg-gray-900">
          <div className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8 py-4">
            <Outlet />
          </div>
        </main>
        
        <footer className="text-center text-sm text-gray-500 dark:text-gray-400 py-3 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
          © 2025 NeuraFlow. Todos los derechos reservados.
        </footer>
      </div>
    </div>
  );
};

export default Layout;
