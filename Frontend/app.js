/* Talks to WebServer.java. If you open this page from a different address
   (e.g. VS Code Live Server), keep API as the full URL of the Java server. */
const API = location.port === "8081" ? "/api" : "http://localhost:8081/api";
const PAGE_SIZE = 8;

const KINDS = {
  students: {
    singular: "student", key: "rollno",
    cols: [["rollno", "Roll no", "number"], ["name", "Name", "text"]],
  },
  teachers: {
    singular: "teacher", key: "subject_id",
    cols: [["subject_id", "Subject ID", "number"], ["name", "Name", "text"], ["subject", "Subject", "text"]],
  },
};

const $ = id => document.getElementById(id);
let token = sessionStorage.getItem("token") || "";
let kind = "students", data = { students: [], teachers: [] };
let sortKey = "rollno", sortDir = 1, page = 1;

const esc = s => { const d = document.createElement("div"); d.textContent = s; return d.innerHTML; };
function toast(msg) {
  const t = $("toast"); t.textContent = msg; t.classList.add("show");
  clearTimeout(toast.t); toast.t = setTimeout(() => t.classList.remove("show"), 2800);
}

async function api(path, opts = {}) {
  let res;
  try {
    res = await fetch(API + path, { ...opts, headers: { "Content-Type": "application/json", Authorization: token } });
  } catch {
    throw new Error("Cannot reach the server. Start WebServer and open http://localhost:8080.");
  }
  const body = await res.json().catch(() => ({}));
  if (res.status === 401 && path !== "/login") { logout(); throw new Error("Session ended. Log in again."); }
  if (!res.ok) throw new Error(body.error || `Server returned ${res.status}`);
  return body;
}

/* ---------- login ---------- */
$("loginForm").onsubmit = async e => {
  e.preventDefault();
  try {
    const r = await api("/login", { method: "POST", body: JSON.stringify({ id: $("loginId").value, password: $("loginPw").value }) });
    token = r.token; sessionStorage.setItem("token", token);
    $("loginErr").hidden = true; start();
  } catch (err) { $("loginErr").textContent = err.message; $("loginErr").hidden = false; }
};
function logout() {
  token = ""; sessionStorage.removeItem("token");
  $("appView").hidden = true; $("loginView").hidden = false; $("loginPw").value = "";
}
$("logout").onclick = logout;

/* ---------- data ---------- */
async function load() {
  try {
    [data.students, data.teachers] = await Promise.all([api("/students"), api("/teachers")]);
    render();
  } catch (err) { $("empty").hidden = false; $("empty").textContent = err.message; }
}
function start() { $("loginView").hidden = true; $("appView").hidden = false; setKind("students"); load(); }

function setKind(k) {
  kind = k; page = 1; sortKey = KINDS[k].key; sortDir = 1; $("search").value = "";
  $("tabStudents").setAttribute("aria-selected", k === "students");
  $("tabTeachers").setAttribute("aria-selected", k === "teachers");
  $("addBtn").textContent = `Add ${KINDS[k].singular}`;
  $("search").placeholder = `Search ${k}`;
  $("head").innerHTML = KINDS[k].cols.map(c => `<th data-col="${c[0]}"><button data-sort="${c[0]}">${c[1]}</button></th>`).join("")
    + '<th><span style="position:absolute;left:-9999px">Actions</span></th>';
  render();
}
$("tabStudents").onclick = () => setKind("students");
$("tabTeachers").onclick = () => setKind("teachers");

function render() {
  const K = KINDS[kind], q = $("search").value.trim().toLowerCase();
  const list = data[kind]
    .filter(r => !q || K.cols.some(c => String(r[c[0]]).toLowerCase().includes(q)))
    .sort((a, b) => {
      const x = a[sortKey], y = b[sortKey];
      return (typeof x === "number" ? x - y : String(x).localeCompare(String(y))) * sortDir;
    });
  const pages = Math.max(1, Math.ceil(list.length / PAGE_SIZE));
  page = Math.min(page, pages);
  $("rows").innerHTML = list.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE).map(r =>
    "<tr>" + K.cols.map(c => `<td>${esc(r[c[0]])}</td>`).join("") +
    `<td class="del"><button class="btn del" data-del="${r[K.key]}">Delete</button></td></tr>`).join("");
  $("empty").hidden = list.length > 0;
  $("empty").textContent = data[kind].length ? "No matches. Clear the search to see everyone."
    : `No ${kind} yet. Select Add ${K.singular} to create the first one.`;
  $("pageInfo").textContent = `Page ${page} of ${pages}`;
  $("prev").disabled = page <= 1; $("next").disabled = page >= pages;
  document.querySelectorAll("#head th[data-col]").forEach(th => {
    th.removeAttribute("aria-sort");
    if (th.dataset.col === sortKey) th.setAttribute("aria-sort", sortDir > 0 ? "ascending" : "descending");
  });
  $("sStudents").textContent = data.students.length;
  $("sTeachers").textContent = data.teachers.length;
  $("sSubjects").textContent = new Set(data.teachers.map(t => t.subject.trim().toLowerCase())).size;
}

/* ---------- table events ---------- */
$("head").onclick = e => {
  const k = e.target.dataset?.sort; if (!k) return;
  sortDir = sortKey === k ? -sortDir : 1; sortKey = k; render();
};
$("search").oninput = () => { page = 1; render(); };
$("prev").onclick = () => { page--; render(); };
$("next").onclick = () => { page++; render(); };

$("rows").onclick = async e => {
  const id = e.target.dataset.del; if (!id) return;
  if (!confirm(`Delete this ${KINDS[kind].singular}? This cannot be undone.`)) return;
  try { await api(`/${kind}/${id}`, { method: "DELETE" }); await load(); toast(`${KINDS[kind].singular} deleted`); }
  catch (err) { toast("Could not delete. " + err.message); }
};

/* ---------- add form ---------- */
$("addBtn").onclick = () => {
  const K = KINDS[kind];
  $("dlgTitle").textContent = `Add ${K.singular}`;
  $("fields").innerHTML = K.cols.map(c =>
    `<label>${c[1]} <input id="f-${c[0]}" type="${c[2]}" ${c[2] === "number" ? 'min="1"' : ""} required></label>`).join("");
  $("formErr").hidden = true; $("dlg").showModal();
  $("f-" + K.cols[0][0]).focus();
};
$("cancel").onclick = () => $("dlg").close();
$("form").onsubmit = async e => {
  e.preventDefault();
  const body = {};
  KINDS[kind].cols.forEach(c => body[c[0]] = c[2] === "number" ? Number($("f-" + c[0]).value) : $("f-" + c[0]).value.trim());
  try {
    await api(`/${kind}`, { method: "POST", body: JSON.stringify(body) });
    $("dlg").close(); await load(); toast(`${KINDS[kind].singular} added`);
  } catch (err) { $("formErr").textContent = err.message; $("formErr").hidden = false; }
};

if (token) start();
