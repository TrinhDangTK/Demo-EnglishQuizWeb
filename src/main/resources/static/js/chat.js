(function () {
  'use strict';

  // ── HTML template (template literal avoids single-quote escaping issues) ──
  var WIDGET_HTML = `
<button id="chat-fab" class="chat-fab" aria-label="Open AI Assistant" title="AI English Assistant">
  <svg class="chat-fab-icon chat-fab-icon--open" xmlns="http://www.w3.org/2000/svg" width="26" height="26"
       viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
       stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
  </svg>
  <svg class="chat-fab-icon chat-fab-icon--close" xmlns="http://www.w3.org/2000/svg" width="22" height="22"
       viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
       stroke-linecap="round" aria-hidden="true">
    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
  </svg>
</button>

<div id="chat-panel" class="chat-panel" aria-live="polite">
  <div class="chat-header">
    <div class="chat-header-info">
      <span class="chat-header-dot"></span>
      <span class="chat-header-title">AI English Assistant</span>
    </div>
    <button id="chat-clear" class="chat-clear-btn" title="New conversation">
      <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
           fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"
           stroke-linejoin="round" aria-hidden="true">
        <polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 .49-3.51"/>
      </svg>
      New chat
    </button>
  </div>

  <div id="chat-messages" class="chat-messages">
    <div class="chat-msg chat-msg--ai">
      <div class="chat-bubble">
        <p>&#x1F44B; Hi! I am your English learning assistant.</p>
        <p>Ask me about grammar, vocabulary, or the question you are working on!</p>
      </div>
    </div>
  </div>

  <div id="chat-context-bar" class="chat-context-bar" style="display:none">
    <span id="chat-context-label" class="chat-context-label"></span>
    <button id="chat-context-dismiss" class="chat-context-dismiss" title="Remove context">&times;</button>
  </div>

  <div class="chat-input-area">
    <textarea id="chat-input" class="chat-input" placeholder="Ask a question..." rows="1" maxlength="800"></textarea>
    <button id="chat-send-btn" class="chat-send-btn" aria-label="Send">
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24"
           fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"
           stroke-linejoin="round" aria-hidden="true">
        <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
      </svg>
    </button>
  </div>
</div>`;

  // ── State ──────────────────────────────────────────────────────────────────
  var isOpen     = false;
  var isLoading  = false;
  var history    = [];
  var quizContext = null;

  // ── DOM refs ───────────────────────────────────────────────────────────────
  var fab, panel, messagesEl, inputEl, sendBtn, clearBtn, contextBar, contextLabel, contextDismiss;

  // ── Build & init ───────────────────────────────────────────────────────────
  function buildWidget() {
    if (document.getElementById('chat-widget')) return; // already mounted
    var wrapper = document.createElement('div');
    wrapper.id = 'chat-widget';
    wrapper.className = 'chat-widget';
    wrapper.innerHTML = WIDGET_HTML;
    document.body.appendChild(wrapper);
  }

  function init() {
    buildWidget();

    fab            = document.getElementById('chat-fab');
    panel          = document.getElementById('chat-panel');
    messagesEl     = document.getElementById('chat-messages');
    inputEl        = document.getElementById('chat-input');
    sendBtn        = document.getElementById('chat-send-btn');
    clearBtn       = document.getElementById('chat-clear');
    contextBar     = document.getElementById('chat-context-bar');
    contextLabel   = document.getElementById('chat-context-label');
    contextDismiss = document.getElementById('chat-context-dismiss');

    // Guard: abort silently if any element is missing (prevents null errors)
    if (!fab || !panel || !messagesEl || !inputEl || !sendBtn || !clearBtn) return;

    fab.addEventListener('click', togglePanel);
    sendBtn.addEventListener('click', sendMessage);
    clearBtn.addEventListener('click', clearChat);

    if (contextDismiss) {
      contextDismiss.addEventListener('click', function () {
        quizContext = null;
        if (contextBar) contextBar.style.display = 'none';
      });
    }

    inputEl.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
    inputEl.addEventListener('input', autoResize);

    detectQuizContext();

    document.addEventListener('click', function (e) {
      var widget = document.getElementById('chat-widget');
      if (isOpen && widget && !widget.contains(e.target)) closePanel();
    });
  }

  // ── Panel toggle ───────────────────────────────────────────────────────────
  function togglePanel() { isOpen ? closePanel() : openPanel(); }

  function openPanel() {
    isOpen = true;
    fab.classList.add('chat-fab--open');
    panel.classList.add('chat-panel--visible');
    inputEl.focus();
  }

  function closePanel() {
    isOpen = false;
    fab.classList.remove('chat-fab--open');
    panel.classList.remove('chat-panel--visible');
  }

  // ── Quiz context ───────────────────────────────────────────────────────────
  function detectQuizContext() {
    var qTitle = document.querySelector('.q-title');
    if (!qTitle) return;

    var qType = document.querySelector('.q-type');
    var ctx = 'Question: ' + qTitle.textContent.trim();
    if (qType) ctx += ' [' + qType.textContent.trim() + ']';

    var answerTexts = [];
    document.querySelectorAll('.answer-item').forEach(function (a) {
      var label = a.querySelector('.al');
      var nodes = a.childNodes;
      var text  = nodes[nodes.length - 1];
      if (label && text) answerTexts.push(label.textContent.trim() + '. ' + (text.textContent || '').trim());
    });
    if (answerTexts.length) ctx += '\nOptions: ' + answerTexts.join(' | ');

    quizContext = ctx;
    if (contextLabel) contextLabel.textContent = '\uD83D\uDCDD Context: current quiz question';
    if (contextBar)   contextBar.style.display = 'flex';
  }

  // ── Send ───────────────────────────────────────────────────────────────────
  function sendMessage() {
    if (isLoading) return;
    var text = inputEl.value.trim();
    if (!text) return;

    appendMsg('user', text);
    history.push({ role: 'user', text: text });
    inputEl.value = '';
    inputEl.style.height = 'auto';

    var loadingEl = appendTyping();
    isLoading = true;
    sendBtn.disabled = true;

    fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: text,
        history: history.slice(0, -1),
        context: quizContext || ''
      })
    })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        loadingEl.remove();
        var reply = data.reply || data.error || 'Something went wrong.';
        appendMsg('ai', reply);
        history.push({ role: 'model', text: reply });
        if (history.length > 20) history = history.slice(-20);
      })
      .catch(function () {
        loadingEl.remove();
        appendMsg('ai', '\u26A0\uFE0F Network error. Please check your connection and try again.');
      })
      .finally(function () {
        isLoading = false;
        sendBtn.disabled = false;
        inputEl.focus();
      });
  }

  // ── Clear ──────────────────────────────────────────────────────────────────
  function clearChat() {
    history = [];
    messagesEl.innerHTML = '';
    appendMsg('ai', '\uD83D\uDD04 New conversation started! Ask me anything about English.');
  }

  // ── DOM helpers ────────────────────────────────────────────────────────────
  function appendMsg(role, text) {
    var row    = document.createElement('div');
    row.className = 'chat-msg chat-msg--' + role;
    var bubble = document.createElement('div');
    bubble.className = 'chat-bubble';
    bubble.innerHTML = renderMarkdown(text);
    row.appendChild(bubble);
    messagesEl.appendChild(row);
    messagesEl.scrollTop = messagesEl.scrollHeight;
    return row;
  }

  function appendTyping() {
    var row = document.createElement('div');
    row.className = 'chat-msg chat-msg--ai';
    row.innerHTML = '<div class="chat-bubble chat-typing"><span></span><span></span><span></span></div>';
    messagesEl.appendChild(row);
    messagesEl.scrollTop = messagesEl.scrollHeight;
    return row;
  }

  function autoResize() {
    this.style.height = 'auto';
    this.style.height = Math.min(this.scrollHeight, 120) + 'px';
  }

  // ── Lightweight markdown ───────────────────────────────────────────────────
  function renderMarkdown(text) {
    var safe = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    safe = safe.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    safe = safe.replace(/\*(.+?)\*/g,     '<em>$1</em>');
    safe = safe.replace(/`([^`]+)`/g,     '<code>$1</code>');

    var lines = safe.split('\n');
    var result = [];
    var inList = false;
    lines.forEach(function (line) {
      var isBullet = /^[-\u2022]\s+/.test(line);
      if (isBullet) {
        if (!inList) { result.push('<ul class="chat-list">'); inList = true; }
        result.push('<li>' + line.replace(/^[-\u2022]\s+/, '') + '</li>');
      } else {
        if (inList) { result.push('</ul>'); inList = false; }
        if (line.trim()) result.push('<p>' + line + '</p>');
      }
    });
    if (inList) result.push('</ul>');
    return result.join('');
  }

  // ── Boot — works whether defer fires before or after DOMContentLoaded ─────
  function boot() {
    if (document.body) {
      init();
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }

})();
