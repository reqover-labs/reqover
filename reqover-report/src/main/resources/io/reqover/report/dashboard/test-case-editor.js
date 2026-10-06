(function () {
  'use strict';
  var form = document.getElementById('reqover-case-form');
  if (!form) { return; }
  var api = window.ReqoverTestDrafts;
  var drafts = [];
  var active = -1;
  var path = document.getElementById('reqover-case-path');
  var status = document.getElementById('reqover-case-expected-status');
  var maximum = document.getElementById('reqover-case-max-ms');
  var review = document.getElementById('reqover-case-reviewed');
  var list = document.getElementById('reqover-case-list');
  var junit = document.getElementById('reqover-case-junit');
  var preview = document.getElementById('reqover-case-preview');

  function text(id, value) { document.getElementById(id).textContent = value; }
  function renderList() {
    list.replaceChildren();
    drafts.forEach(function (draft, index) {
      var button = document.createElement('button'); button.type = 'button';
      button.setAttribute('aria-pressed', String(index === active));
      var name = document.createElement('strong'); name.textContent = draft.source.endpoint;
      var sub = document.createElement('small'); sub.textContent = 'Case ' + (index + 1) + ' / ' + draft.state;
      button.append(name, sub);
      button.addEventListener('click', function () { select(index); });
      list.append(button);
    });
    text('reqover-case-count', drafts.length + (drafts.length === 1 ? ' draft' : ' drafts'));
  }
  function renderState() {
    var draft = drafts[active];
    var issues = api.problems(draft);
    var problems = document.getElementById('reqover-case-problems');
    problems.replaceChildren();
    issues.forEach(function (issue) { var item = document.createElement('li'); item.textContent = issue; problems.append(item); });
    text('reqover-case-state', issues.length ? 'Review required' : 'Reviewed draft');
    document.getElementById('reqover-case-state').classList.toggle('reviewed', !issues.length);
    junit.disabled = issues.length > 0;
    preview.textContent = issues.length ? '' : api.junit(draft, 'ReqoverCase' + (active + 1) + 'Test');
    renderList();
  }
  function select(index) {
    active = index;
    var draft = drafts[index];
    document.getElementById('reqover-case-empty').hidden = true;
    document.getElementById('reqover-case-workspace').hidden = false;
    text('reqover-case-title', draft.source.endpoint);
    text('reqover-case-source', draft.source.requestId);
    text('reqover-case-observed', draft.source.observedStatus === null ? 'Unknown' : 'HTTP ' + draft.source.observedStatus);
    text('reqover-case-interval', draft.source.recordedProcessingMillis === null ? 'Not timed' : draft.source.recordedProcessingMillis.toFixed(2) + ' ms');
    path.value = draft.request.path;
    status.value = draft.expected.statusCode === null ? '' : draft.expected.statusCode;
    maximum.value = draft.expected.maximumClientMillis === null ? '' : draft.expected.maximumClientMillis;
    review.checked = draft.reviewed;
    renderState();
  }
  function save() {
    if (active < 0) { return; }
    drafts[active] = api.update(drafts[active], { path: path.value, expectedStatus: status.value,
      maximumClientMillis: maximum.value, reviewed: review.checked });
    renderState();
  }
  function download(name, value, type) {
    var url = URL.createObjectURL(new Blob([value], { type: type }));
    var link = document.createElement('a'); link.href = url; link.download = name; link.click();
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
  }
  form.addEventListener('submit', function (event) { event.preventDefault(); });
  [path, status, maximum].forEach(function (input) {
    input.addEventListener('input', function () { review.checked = false; save(); });
  });
  review.addEventListener('change', save);
  document.getElementById('reqover-case-json').addEventListener('click', function () {
    if (active < 0) { return; }
    download('reqover-case-' + (active + 1) + '.json', JSON.stringify(drafts[active], null, 2) + '\n', 'application/json');
  });
  junit.addEventListener('click', function () {
    if (active < 0 || api.problems(drafts[active]).length) { return; }
    var name = 'ReqoverCase' + (active + 1) + 'Test';
    download(name + '.java', api.junit(drafts[active], name), 'text/plain;charset=utf-8');
  });
  window.ReqoverCaseEditor = { create: function (request) {
    drafts.push(api.create(request)); select(drafts.length - 1);
  } };
})();
