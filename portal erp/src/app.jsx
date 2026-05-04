/* app.jsx — Top-level router + tweaks integration */

const { useState: useStateA, useEffect: useEffectA, useRef: useRefA } = React;

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "primary": "#4F46E5",
  "accent": "#A78BFA",
  "fontSans": "Inter",
  "fontDisplay": "Space Grotesk",
  "radius": 10,
  "density": "comfortable",
  "heroVariant": "typewriter",
  "showSpeakerNotes": false
}/*EDITMODE-END*/;

function applyTweaks(t) {
  const r = document.documentElement;
  r.style.setProperty('--iv-primary', t.primary);
  r.style.setProperty('--iv-accent', t.accent);
  r.style.setProperty('--iv-radius', t.radius + 'px');
  r.style.setProperty('--iv-font-sans', `"${t.fontSans}", system-ui, sans-serif`);
  r.style.setProperty('--iv-font-display', `"${t.fontDisplay}", "${t.fontSans}", sans-serif`);
  if (t.density === 'compact') {
    r.style.setProperty('--iv-density-pad', '8px');
  } else {
    r.style.setProperty('--iv-density-pad', '12px');
  }
}

function App() {
  const [page, setPage] = useStateA(() => location.hash.replace('#', '') || 'landing');
  const [portalPage, setPortalPage] = useStateA('dashboard');
  const tweaks = useTweaks ? useTweaks(TWEAK_DEFAULTS) : [TWEAK_DEFAULTS, () => {}];
  const [tweakState, setTweak] = tweaks;

  const goto = React.useCallback((target) => {
    window.location.hash = target;
    setPage(target);
    window.scrollTo(0, 0);
  }, []);

  useEffectA(() => { applyTweaks(tweakState); }, [tweakState]);
  useEffectA(() => {
    const onHash = () => setPage(location.hash.replace('#', '') || 'landing');
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  return (
    <LangProvider>
      <ToastProvider>
        {page === 'landing' && <LandingPage goto={goto} heroVariant={tweakState.heroVariant} />}
        {page === 'login' && <LoginPage goto={goto} />}
        {page === 'unauthorized' && <UnauthorizedPage goto={goto} />}
        {page === 'portal' && <PortalShell goto={goto} currentPage={portalPage} setPortalPage={setPortalPage} />}
        {typeof TweaksPanel !== 'undefined' && (
          <TweaksPanel title="Tweaks · InsightVision">
            <TweakSection title="Color">
              <TweakColor label="Primary" value={tweakState.primary} onChange={v => setTweak('primary', v)} />
              <TweakColor label="Accent"  value={tweakState.accent}  onChange={v => setTweak('accent', v)} />
            </TweakSection>
            <TweakSection title="Typography">
              <TweakSelect label="Sans"    value={tweakState.fontSans}    options={['Inter','Geist','IBM Plex Sans','Manrope','Sora']}                 onChange={v => setTweak('fontSans', v)} />
              <TweakSelect label="Display" value={tweakState.fontDisplay} options={['Space Grotesk','Geist','Sora','Manrope','Fraunces','IBM Plex Sans']} onChange={v => setTweak('fontDisplay', v)} />
            </TweakSection>
            <TweakSection title="Layout">
              <TweakSlider label="Radius"  value={tweakState.radius} min={2} max={20} step={1} onChange={v => setTweak('radius', v)} />
              <TweakRadio  label="Density" value={tweakState.density} options={['compact','comfortable']} onChange={v => setTweak('density', v)} />
            </TweakSection>
            <TweakSection title="Hero">
              <TweakRadio label="Variant" value={tweakState.heroVariant} options={['typewriter','terminal','split']} onChange={v => setTweak('heroVariant', v)} />
            </TweakSection>
          </TweaksPanel>
        )}
      </ToastProvider>
    </LangProvider>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
