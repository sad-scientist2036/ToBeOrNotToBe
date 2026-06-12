let seats = [];
let eventSource = null;
let pendingSeatId = null;
let refreshInterval = null;

async function refreshSeats() {
    try {
        const response = await fetch('/api/seats');
        if (!response.ok) throw new Error('Ошибка загрузки');
        const newSeats = await response.json();

        if (JSON.stringify(seats) !== JSON.stringify(newSeats)) {
            seats = newSeats;
            renderHall();
            console.log('Схема обновлена, мест: ' + seats.length);
        }
    } catch (error) {
        console.error('Ошибка обновления схемы:', error);
    }
}

function renderHall() {
    const hallDiv = document.getElementById('hall');
    hallDiv.innerHTML = '';

    if (!seats || !seats.length) {
        hallDiv.innerHTML = '<div class="loading">Загрузка...</div>';
        return;
    }

    let currentRow = 0;
    let rowDiv = null;
    let seatsRow = null;

    for (const seat of seats) {
        if (seat.row !== currentRow) {
            currentRow = seat.row;
            rowDiv = document.createElement('div');
            rowDiv.className = 'row';

            const title = document.createElement('div');
            title.className = 'row-title';
            title.innerText = 'Ряд ' + currentRow;
            rowDiv.appendChild(title);

            seatsRow = document.createElement('div');
            seatsRow.className = 'seats-row';
            rowDiv.appendChild(seatsRow);

            hallDiv.appendChild(rowDiv);
        }

        const seatDiv = document.createElement('div');
        let statusClass = '';
        if (seat.status === 'FREE') statusClass = 'free';
        else if (seat.status === 'HOLD') statusClass = 'hold';
        else statusClass = 'booked';

        seatDiv.className = 'seat ' + statusClass;
        seatDiv.innerText = seat.number;

        if (seat.status === 'FREE') {
            seatDiv.onclick = (function(id) {
                return function() { startBooking(id); };
            })(seat.id);
        } else if (seat.status === 'HOLD') {
            if (pendingSeatId === seat.id) {
                seatDiv.title = 'Продолжить бронирование';
                seatDiv.onclick = (function(id) {
                    return function() { continueBooking(id); };
                })(seat.id);
            } else {
                seatDiv.title = 'Временно забронировано другим пользователем';
                seatDiv.onclick = null;
            }
        } else {
            seatDiv.title = 'Уже занято';
            seatDiv.onclick = null;
        }

        seatsRow.appendChild(seatDiv);
    }
}

async function startBooking(seatId) {
    pendingSeatId = seatId;

    try {
        showMessage('Бронирование места...', 'info');

        const holdResponse = await fetch('/api/seats/' + seatId + '/hold', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const holdData = await holdResponse.json();

        if (!holdResponse.ok) {
            showMessage(holdData.message || 'Не удалось забронировать место', 'error');
            pendingSeatId = null;
            await refreshSeats();
            return;
        }

        await refreshSeats();
        await showBookingDialog(seatId);

    } catch (error) {
        console.error('Ошибка:', error);
        showMessage('Ошибка соединения с сервером', 'error');
        pendingSeatId = null;
        await refreshSeats();
    }
}

async function continueBooking(seatId) {
    showMessage('Продолжение бронирования...', 'info');
    await showBookingDialog(seatId);
}

async function showBookingDialog(seatId) {
    const name = prompt('Введите ваше имя:');
    if (!name || name.trim() === '') {
        await cancelHold(seatId);
        return;
    }

    const phone = prompt('Введите ваш телефон (только цифры, минимум 10):');
    if (!phone || phone.trim() === '') {
        await cancelHold(seatId);
        return;
    }

    const cleanPhone = phone.replace(/[^0-9]/g, '');
    if (cleanPhone.length < 10) {
        showMessage('Введите корректный номер телефона (минимум 10 цифр)', 'error');
        await cancelHold(seatId);
        return;
    }

    showMessage('Подтверждение бронирования...', 'info');

    try {
        const confirmResponse = await fetch('/api/seats/' + seatId + '/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                customerName: name.trim(),
                customerPhone: cleanPhone
            })
        });

        const confirmData = await confirmResponse.json();

        if (confirmResponse.ok) {
            showMessage('Билет №' + confirmData.ticketId + ' успешно оформлен!', 'success');
            pendingSeatId = null;
            await refreshSeats();
            await loadMyBookings();
        } else {
            showMessage(confirmData.message || 'Ошибка при подтверждении', 'error');
            await cancelHold(seatId);
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showMessage('Ошибка соединения с сервером', 'error');
        await cancelHold(seatId);
    }
}

async function cancelHold(seatId) {
    try {
        await fetch('/api/seats/' + seatId + '/cancel-hold', { method: 'POST' });
        showMessage('Бронирование отменено', 'info');
        pendingSeatId = null;
        await refreshSeats();
    } catch (error) {
        console.error('Ошибка отмены:', error);
    }
}

function startSSE() {
    if (eventSource) {
        eventSource.close();
    }

    eventSource = new EventSource('/api/seats/stream');

    eventSource.addEventListener('seats-update', function(event) {
        console.log('SSE обновление получено');
        seats = JSON.parse(event.data);
        renderHall();
    });

    eventSource.onerror = function() {
        console.error('SSE ошибка');
    };
}

async function loadStats() {
    try {
        const response = await fetch('/api/stats');
        const stats = await response.json();
        document.getElementById('stats').innerHTML =
            'Всего мест: ' + stats.totalSeats + ' | ' +
            'Занято: ' + stats.bookedSeats + ' | ' +
            'Свободно: ' + stats.freeSeats + ' | ' +
            'Занятость: ' + stats.occupancyPercentage + '%';
    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
    }
}

async function loadMyBookings() {
    try {
        const response = await fetch('/api/my/bookings');
        const bookings = await response.json();

        const card = document.getElementById('myBookingsCard');
        const listDiv = document.getElementById('myBookingsList');

        if (!bookings || bookings.length === 0) {
            card.style.display = 'none';
            return;
        }

        card.style.display = 'block';
        listDiv.innerHTML = '';

        bookings.forEach(booking => {
            const item = document.createElement('div');
            item.className = 'booking-item';
            item.innerHTML = `<span>Место: ряд ${booking.row}, место ${booking.number}</span>
                              <button onclick="cancelBooking(${booking.seatId})">Отменить</button>`;
            listDiv.appendChild(item);
        });

    } catch (error) {
        console.error('Ошибка загрузки броней:', error);
    }
}

async function cancelBooking(seatId) {
    if (confirm('Отменить бронирование?')) {
        const response = await fetch('/api/seats/' + seatId + '/release', { method: 'POST' });
        if (response.ok) {
            showMessage('Бронирование отменено, место освобождено', 'success');
            await refreshSeats();
            await loadMyBookings();
        } else {
            showMessage('Ошибка отмены бронирования', 'error');
        }
    }
}

function showMessage(msg, type) {
    const msgDiv = document.getElementById('message');
    msgDiv.textContent = msg;
    msgDiv.className = 'message ' + type;
    setTimeout(() => {
        if (msgDiv.textContent === msg) {
            msgDiv.textContent = '';
            msgDiv.className = 'message';
        }
    }, 5000);
}

document.getElementById('statsBtn').onclick = loadStats;
document.getElementById('refreshBtn').onclick = refreshSeats;
document.getElementById('myBookingsBtn').onclick = loadMyBookings;

startSSE();
refreshSeats();
refreshInterval = setInterval(refreshSeats, 3000);
loadStats();
loadMyBookings();