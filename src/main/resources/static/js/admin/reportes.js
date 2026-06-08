/* admin/reportes.js — Gerenciamento de Reportes de Problema */

var _todos = [];
var _filtroAtual = 'todos';

function esc(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function badgeStatus(status) {
    var cores = {
        'ATIVO':    { bg: '#e8f5e9', color: '#2e7d32' },
        'SUSPENSO': { bg: '#fff3e0', color: '#e65100' },
        'REMOVIDO': { bg: '#fce4ec', color: '#c62828' }
    };
    var c = cores[status] || { bg: '#f5f5f5', color: '#757575' };
    return '<span style="background:' + c.bg + ';color:' + c.color + ';padding:2px 10px;border-radius:20px;font-size:0.75rem;font-weight:600;">' + esc(status) + '</span>';
}

function setFiltro(filtro) {
    _filtroAtual = filtro;
    document.getElementById('btnTodos').style.background    = filtro === 'todos'     ? '#722f37' : '#fff';
    document.getElementById('btnTodos').style.color         = filtro === 'todos'     ? '#fff'    : '#7a6e65';
    document.getElementById('btnTodos').style.borderColor   = filtro === 'todos'     ? '#722f37' : '#e0d9d0';
    document.getElementById('btnNaoLidos').style.background = filtro === 'nao-lidos' ? '#722f37' : '#fff';
    document.getElementById('btnNaoLidos').style.color      = filtro === 'nao-lidos' ? '#fff'    : '#7a6e65';
    document.getElementById('btnNaoLidos').style.borderColor= filtro === 'nao-lidos' ? '#722f37' : '#e0d9d0';
    var lista = filtro === 'nao-lidos' ? _todos.filter(function(r) { return !r.lido; }) : _todos;
    renderTabela(lista);
}

function renderTabela(lista) {
    var tbody = document.getElementById('tbodyReportes');
    if (!lista || lista.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="padding:2rem;text-align:center;color:#aaa;">Nenhum reporte encontrado.</td></tr>';
        return;
    }
    tbody.innerHTML = lista.map(function(r) {
        var rowBg = r.lido ? '' : 'background:#fffbf5;';
        var lidoBadge = r.lido
            ? '<span style="font-size:0.72rem;color:#888;font-style:italic;">lido</span>'
            : '<span style="background:#722f37;color:#fff;border-radius:10px;padding:1px 7px;font-size:0.72rem;font-weight:700;">novo</span>';
        var respostaBadge = (r.respostas && r.respostas.length > 0)
            ? ' <span style="background:#e8f5e9;color:#2e7d32;border-radius:10px;padding:1px 6px;font-size:0.7rem;font-weight:600;">' + r.respostas.length + ' resp.</span>'
            : '';
        return '<tr style="border-bottom:1px solid #f0ebe4;' + rowBg + '">'
            + '<td style="padding:0.75rem 1rem;white-space:nowrap;color:#2c241b;">'
            +   esc(r.dataCriacao) + '<br>' + lidoBadge + respostaBadge
            + '</td>'
            + '<td style="padding:0.75rem 1rem;color:#2c241b;">' + esc(r.emailContato) + '</td>'
            + '<td style="padding:0.75rem 1rem;color:#2c241b;max-width:200px;">'
            +   '<div style="font-weight:600;">' + esc(r.motivo) + '</div>'
            +   (r.detalhes ? '<div style="font-size:0.78rem;color:#7a6e65;margin-top:4px;">' + esc(r.detalhes) + '</div>' : '')
            + '</td>'
            + '<td style="padding:0.75rem 1rem;color:#2c241b;">' + esc(r.nomeUsuario) + '</td>'
            + '<td style="padding:0.75rem 1rem;">'
            +   (r.statusConta !== '—' ? badgeStatus(r.statusConta) : '<span style="color:#aaa;">—</span>')
            + '</td>'
            + '<td style="padding:0.75rem 1rem;">'
            +   '<button onclick="abrirDetalhes(' + r.id + ')" style="padding:0.35rem 0.75rem;border:none;border-radius:6px;background:#722f37;color:#fff;font-size:0.78rem;cursor:pointer;font-weight:600;">Detalhes</button>'
            + '</td>'
            + '</tr>';
    }).join('');
}

function abrirDetalhes(id) {
    var r = _todos.find(function(x) { return x.id === id; });
    if (!r) return;

    if (!r.lido) {
        fetch('/api/admin/reportes/' + id + '/marcar-lido', {
            method: 'POST', credentials: 'include'
        }).then(function(res) {
            if (res.ok) {
                r.lido = true;
                renderTabela(_filtroAtual === 'nao-lidos'
                    ? _todos.filter(function(x) { return !x.lido; })
                    : _todos);
            }
        }).catch(function() {});
    }

    document.getElementById('detEmail').textContent   = r.emailContato || '—';
    document.getElementById('detMotivo').textContent  = r.motivo || '—';
    document.getElementById('detData').textContent    = r.dataCriacao || '—';
    document.getElementById('detStatus').innerHTML    = (r.statusConta && r.statusConta !== '—')
        ? badgeStatus(r.statusConta)
        : '<span style="color:#aaa;">—</span>';
    document.getElementById('detMembro').textContent  = r.dataCadastro || '—';
    document.getElementById('detUsuario').textContent = r.nomeUsuario || '—';
    document.getElementById('detDetalhes').textContent = r.detalhes || '(sem detalhes adicionais)';

    var hist = document.getElementById('detHistorico');
    if (r.respostas && r.respostas.length > 0) {
        hist.innerHTML = r.respostas.map(function(resp) {
            return '<div style="background:#f9f6f0;border-left:3px solid #722f37;'
                + 'padding:0.75rem 1rem;margin-bottom:0.5rem;border-radius:0 6px 6px 0;">'
                + '<div style="font-size:0.78rem;color:#7a6e65;margin-bottom:0.3rem;">' + esc(resp.dataEnvio) + '</div>'
                + '<div style="font-size:0.875rem;color:#2c241b;white-space:pre-wrap;">' + esc(resp.mensagem) + '</div>'
                + '</div>';
        }).join('');
    } else {
        hist.innerHTML = '<p style="color:#aaa;font-size:0.85rem;font-style:italic;margin:0;">Nenhuma resposta enviada ainda.</p>';
    }

    document.getElementById('modalDetalhesReporte').dataset.reporteId = id;
    document.getElementById('detRespMensagem').value = '';
    document.getElementById('modalDetalhesReporte').style.display = 'flex';
}

function fecharDetalhes() {
    document.getElementById('modalDetalhesReporte').style.display = 'none';
}

function confirmarEnvioResposta() {
    var id = Number(document.getElementById('modalDetalhesReporte').dataset.reporteId);
    var mensagem = document.getElementById('detRespMensagem').value.trim();
    if (!mensagem) { alert('Informe a mensagem.'); return; }

    mostrarConfirm(
        'Confirmar envio',
        'Enviar e-mail de resposta ao usuário?',
        function() {
            fetch('/api/admin/reportes/' + id + '/responder', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ mensagem: mensagem })
            }).then(function(res) {
                if (res.ok) {
                    fecharDetalhes();
                    carregarDados();
                    alert('Resposta enviada com sucesso!');
                } else {
                    alert('Erro ao enviar resposta. Tente novamente.');
                }
            }).catch(function() { alert('Erro ao enviar resposta. Tente novamente.'); });
        }
    );
}

function confirmarExclusao() {
    var id = Number(document.getElementById('modalDetalhesReporte').dataset.reporteId);
    mostrarConfirm(
        'Excluir reporte',
        'Tem certeza que deseja excluir este reporte permanentemente?',
        function() {
            fetch('/api/admin/reportes/' + id, {
                method: 'DELETE', credentials: 'include'
            }).then(function(res) {
                if (res.ok) {
                    _todos = _todos.filter(function(x) { return x.id !== id; });
                    fecharDetalhes();
                    setFiltro(_filtroAtual);
                } else {
                    alert('Erro ao excluir reporte.');
                }
            }).catch(function() { alert('Erro ao excluir reporte.'); });
        }
    );
}

function mostrarConfirm(titulo, texto, onOk) {
    document.getElementById('confirmTitulo').textContent = titulo;
    document.getElementById('confirmTexto').textContent  = texto;
    var modal = document.getElementById('modalConfirmReporte');
    modal.style.display = 'flex';
    document.getElementById('confirmOkBtn').onclick = function() {
        modal.style.display = 'none';
        onOk();
    };
    document.getElementById('confirmCancelarBtn').onclick = function() {
        modal.style.display = 'none';
    };
}

function carregarDados() {
    fetch('/api/admin/reportes', { credentials: 'include' })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            _todos = data;
            setFiltro(_filtroAtual);
            var naoLidos = _todos.filter(function(r) { return !r.lido; }).length;
            var badge = document.getElementById('badgeNaoLidos');
            if (badge) {
                badge.textContent = naoLidos;
                badge.style.display = naoLidos > 0 ? 'inline' : 'none';
            }
        }).catch(function() {
            document.getElementById('tbodyReportes').innerHTML =
                '<tr><td colspan="6" style="padding:2rem;text-align:center;color:#e57373;">Erro ao carregar reportes.</td></tr>';
        });
}

document.addEventListener('DOMContentLoaded', function() {
    carregarDados();
});
