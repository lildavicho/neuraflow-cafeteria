import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import styled from 'styled-components'
import PropTypes from 'prop-types'

const PurchaseCard = ({ onComplete }) => {
  const backdropRef = useRef(null)
  const innerRef = useRef(null)

  useEffect(() => {
    const tl = gsap.timeline({
      onComplete: () => {
        setTimeout(() => {
          onComplete?.()
        }, 700)
      }
    })

    // Fade-in backdrop and card
    tl.fromTo(
      backdropRef.current,
      { opacity: 0 },
      { opacity: 1, duration: 0.25, ease: 'power2.out' }
    )
      .fromTo(
        innerRef.current,
        { scale: 0.9, opacity: 0 },
        { scale: 1, opacity: 1, duration: 0.35, ease: 'back.out(1.6)' },
        '<'
      )

    // Trigger CSS keyframes (formerly hover) programmatically
    const el = innerRef.current
    const addPlay = () => el && el.classList.add('play')
    const removePlay = () => el && el.classList.remove('play')
    setTimeout(addPlay, 150)
    // Remove class before unmount to reset
    return () => {
      tl.kill()
      removePlay()
    }
  }, [onComplete])

  return (
    <StyledWrapper ref={backdropRef}>
      <div className="container" ref={innerRef}>
        <div className="left-side">
          <div className="card">
            <div className="card-line" />
            <div className="buttons" />
          </div>
          <div className="post">
            <div className="post-line" />
            <div className="screen">
              <div className="dollar">$</div>
            </div>
            <div className="numbers" />
            <div className="numbers-line2" />
          </div>
        </div>
        <div className="right-side">
          <div className="new">Nueva Transacción</div>
          <svg viewBox="0 0 451.846 451.847" className="arrow">
            <path
              fill="#cfcfcf"
              d="M345.441 248.292L151.154 442.573c-12.359 12.365-32.397 12.365-44.75 0-12.354-12.354-12.354-32.391 0-44.744L278.318 225.92 106.409 54.017c-12.354-12.359-12.354-32.394 0-44.748 12.354-12.359 32.391-12.359 44.75 0l194.287 194.284c6.177 6.18 9.262 14.271 9.262 22.366 0 8.099-3.091 16.196-9.267 22.373z"
            />
          </svg>
        </div>
      </div>
    </StyledWrapper>
  )
}

PurchaseCard.propTypes = {
  onComplete: PropTypes.func
}

const StyledWrapper = styled.div`
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;

  .container {
    background: #fff;
    display: flex;
    width: 460px;
    height: 120px;
    position: relative;
    border-radius: 6px;
    transition: 0.3s;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  }

  .container.play {
    transform: scale(1.03);
    width: 220px;
  }

  .container.play .left-side {
    width: 100%;
  }

  .left-side {
    background: #5de2a3;
    width: 130px;
    height: 120px;
    border-radius: 4px;
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    transition: 0.3s;
    flex-shrink: 0;
    overflow: hidden;
  }

  .right-side {
    width: calc(100% - 130px);
    display: flex;
    align-items: center;
    overflow: hidden;
    cursor: pointer;
    justify-content: space-between;
    white-space: nowrap;
    transition: 0.3s;
  }

  .container.play .right-side { background: #f9f7f9; }

  .arrow {
    width: 20px;
    height: 20px;
    margin-right: 20px;
  }

  .new {
    font-size: 23px;
    margin-left: 20px;
    color: #333;
    font-weight: 600;
  }

  .card {
    width: 70px;
    height: 46px;
    background: #c7ffbc;
    border-radius: 6px;
    position: absolute;
    display: flex;
    z-index: 10;
    flex-direction: column;
    align-items: center;
    box-shadow: 9px 9px 9px -2px rgba(77, 200, 143, 0.72);
  }

  .card-line {
    width: 65px;
    height: 13px;
    background: #80ea69;
    border-radius: 2px;
    margin-top: 7px;
  }

  .buttons {
    width: 8px;
    height: 8px;
    background: #379e1f;
    box-shadow: 0 -10px 0 0 #26850e, 0 10px 0 0 #56be3e;
    border-radius: 50%;
    margin-top: 5px;
    transform: rotate(90deg);
    margin: 10px 0 0 -30px;
  }

  .container.play .card {
    animation: slide-top 1.2s cubic-bezier(0.645, 0.045, 0.355, 1) both;
  }

  @keyframes slide-top {
    0% {
      transform: translateY(0);
    }
    50% {
      transform: translateY(-70px) rotate(90deg);
    }
    60% {
      transform: translateY(-70px) rotate(90deg);
    }
    100% {
      transform: translateY(-8px) rotate(90deg);
    }
  }

  .post {
    width: 63px;
    height: 75px;
    background: #dddde0;
    position: absolute;
    z-index: 11;
    bottom: 10px;
    top: 120px;
    border-radius: 6px;
    overflow: hidden;
  }

  .post-line {
    width: 47px;
    height: 9px;
    background: #545354;
    position: absolute;
    border-radius: 0 0 3px 3px;
    right: 8px;
    top: 8px;
  }

  .post-line:before {
    content: '';
    position: absolute;
    width: 47px;
    height: 9px;
    background: #757375;
    top: -8px;
  }

  .screen {
    width: 47px;
    height: 23px;
    background: #fff;
    position: absolute;
    top: 22px;
    right: 8px;
    border-radius: 3px;
  }

  .numbers,
  .numbers-line2 {
    width: 12px;
    height: 12px;
    border-radius: 2px;
    position: absolute;
    transform: rotate(90deg);
    left: 25px;
  }

  .numbers {
    background: #838183;
    box-shadow: 0 -18px 0 0 #838183, 0 18px 0 0 #838183;
    top: 52px;
  }

  .numbers-line2 {
    background: #aaa9ab;
    box-shadow: 0 -18px 0 0 #aaa9ab, 0 18px 0 0 #aaa9ab;
    top: 68px;
  }

  .dollar {
    position: absolute;
    font-size: 16px;
    width: 100%;
    left: 0;
    top: 0;
    color: #4b953b;
    text-align: center;
    font-weight: bold;
  }

  .container.play .dollar {
    animation: fade-in 0.3s 1s backwards;
  }

  @keyframes fade-in {
    0% {
      opacity: 0;
      transform: translateY(-5px);
    }
    100% {
      opacity: 1;
      transform: translateY(0);
    }
  }
`

export default PurchaseCard
