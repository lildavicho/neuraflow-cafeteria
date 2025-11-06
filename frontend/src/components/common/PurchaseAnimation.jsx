import { useEffect, useRef } from 'react'
import gsap from 'gsap'

const PurchaseAnimation = ({ onComplete }) => {
  const containerRef = useRef(null)

  useEffect(() => {
    if (containerRef.current) {
      const tl = gsap.timeline({
        onComplete: () => {
          setTimeout(() => {
            onComplete?.()
          }, 1000)
        }
      })

      tl.fromTo(
        containerRef.current,
        { scale: 0, opacity: 0 },
        { scale: 1, opacity: 1, duration: 0.3, ease: 'back.out(1.7)' }
      )

      const card = containerRef.current.querySelector('.purchase-card')
      if (card) {
        tl.to(card, { rotation: 90, y: -70, duration: 0.6, delay: 0.5 }, '+=0.2')
      }
    }
  }, [onComplete])

  return (
    <div className="fixed inset-0 bg-black/50 z-[9999] flex items-center justify-center">
      <div ref={containerRef} className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-2xl max-w-md w-full">
        <div className="flex items-center justify-between mb-6">
          <div>
            <div className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-1">
              Nueva Transacción
            </div>
            <div className="text-sm text-gray-600 dark:text-gray-400">
              Procesando tu pedido...
            </div>
          </div>
          <div className="w-16 h-16 bg-success-100 dark:bg-success-900/30 rounded-full flex items-center justify-center">
            <svg className="w-8 h-8 text-success-600 dark:text-success-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
        </div>

        <div className="relative h-32 flex items-center justify-center overflow-hidden">
          <div className="purchase-card absolute w-20 h-12 bg-gradient-to-br from-success-400 to-success-600 rounded-lg shadow-lg">
            <div className="w-full h-3 bg-white/30 rounded-t-lg mt-2" />
            <div className="flex items-center justify-center h-full -mt-3">
              <div className="text-white text-2xl font-bold">$</div>
            </div>
          </div>

          <div className="absolute bottom-0 w-16 h-20 bg-gray-200 dark:bg-gray-700 rounded-t-lg">
            <div className="w-12 h-2 bg-gray-400 dark:bg-gray-600 rounded-full mx-auto mt-2" />
            <div className="w-12 h-6 bg-white dark:bg-gray-800 rounded mx-auto mt-2 flex items-center justify-center">
              <div className="text-xs text-success-600 dark:text-success-400 font-bold">✓</div>
            </div>
          </div>
        </div>

        <div className="mt-8 flex items-center justify-center">
          <div className="flex gap-1">
            <div className="w-2 h-2 bg-brand rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
            <div className="w-2 h-2 bg-brand rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
            <div className="w-2 h-2 bg-brand rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
          </div>
        </div>
      </div>
    </div>
  )
}

export default PurchaseAnimation
