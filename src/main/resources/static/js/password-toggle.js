(function () {
  function init() {
    document.querySelectorAll('.password-wrap').forEach(function (wrap) {
      var input  = wrap.querySelector('input');
      var btn    = wrap.querySelector('.password-toggle');
      if (!input || !btn) return;

      var eyeOpen = btn.querySelector('.icon-eye');
      var eyeOff  = btn.querySelector('.icon-eye-off');

      btn.addEventListener('click', function () {
        var showing = input.type === 'text';
        input.type = showing ? 'password' : 'text';
        if (eyeOpen) eyeOpen.style.display = showing ? 'none'  : 'block';
        if (eyeOff)  eyeOff.style.display  = showing ? 'block' : 'none';
        btn.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
        btn.setAttribute('title',      showing ? 'Show password' : 'Hide password');
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
