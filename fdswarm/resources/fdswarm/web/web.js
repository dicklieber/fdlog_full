/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

function toUpperCase(input) {
    input.value = input.value.toUpperCase();
}

function setSection(code) {
    const sectionInput = document.getElementById('sectionInput');
    if (sectionInput) {
        sectionInput.value = code.toUpperCase();
        // Trigger submit
        const form = sectionInput.closest('form');
        if (form) {
            form.submit();
        }
    }
}

function setupAutoTab() {
    const callsignInput = document.getElementById('callsignInput');
    const classInput = document.querySelector('input[name="contestClass"]');
    const sectionInput = document.getElementById('sectionInput');

    if (callsignInput && classInput && sectionInput) {
        callsignInput.addEventListener('keydown', (e) => {
            if (e.key === ' ' || e.key === 'Enter' || e.key === 'Tab') {
                if (callsignInput.value.length >= 3) {
                    e.preventDefault();
                    classInput.focus();
                }
            } else if (e.key.length === 1 && e.key.match(/[0-9]/)) {
                // If it's a digit, we check if the current callsign is already valid.
                // If it is, then typing another digit is actually the start of the Class field.
                // Standard format: [A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}
                // Regex for valid callsign: ^(?=.{3,12}$)[A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}(?:/[A-Z0-9]{1,4})?$
                const currentVal = callsignInput.value.toUpperCase();
                const callsignRegex = /^(?=.{3,12}$)[A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}(?:\/[A-Z0-9]{1,4})?$/;
                if (callsignRegex.test(currentVal)) {
                    e.preventDefault();
                    classInput.value = e.key;
                    classInput.focus();
                }
            }
        });

        classInput.addEventListener('keydown', (e) => {
            if (e.key === ' ' || e.key === 'Enter' || e.key === 'Tab' || (e.key.length === 1 && e.key.match(/[a-zA-Z]/))) {
                // If it's a letter, we might want to include it if it's the class char
                // But simple approach: if it's already valid or becomes valid
                setTimeout(() => {
                    const val = classInput.value;
                    if (val.match(/^[0-9]{1,2}[A-Z]$/)) {
                        sectionInput.focus();
                    }
                }, 10);
            }
        });

        sectionInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                const form = sectionInput.closest('form');
                if (form) {
                    form.submit();
                }
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', setupAutoTab);

function fetchDups(input) {
    const qsoPart = input.value;
    const dupsRow = document.getElementById('dupsRow');
    const dupsContainer = document.getElementById('dupsContainer');
    
    if (qsoPart.length < 2) {
        dupsRow.style.display = 'none';
        dupsContainer.innerHTML = '';
        return;
    }

    fetch(`/web/dups?qsoPart=${encodeURIComponent(qsoPart)}`)
        .then(response => response.text())
        .then(html => {
            if (html.trim() === '<div></div>' || html.trim() === '') {
                dupsRow.style.display = 'none';
                dupsContainer.innerHTML = '';
            } else {
                dupsRow.style.display = 'block';
                dupsContainer.innerHTML = html;
            }
        })
        .catch(error => console.error('Error fetching dups:', error));
}
