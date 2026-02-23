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
