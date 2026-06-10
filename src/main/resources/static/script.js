let seats = [];
let eventSource = null;
let holdTimer = null;
let holdTimeout = null;
let currentHoldSeatId = null;

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
            seatDiv.title = 'Временно забронировано';
        } else {
            seatDiv.title = 'Уже занято';
        }

        seatsRow.appendChild(seatDiv);
    }
}

function startSSE() {
    if (eventSource) {
        eventSource.close();
    }

    eventSource = new EventSource('/api/seats/stream');

    eventSource.addEventListener('seats-update', function(event) {
        seats = JSON.parse(event.data);
        renderHall();
    });

    eventSource.onerror = function() {
        console.error('SSE ошибка, переподключение через 5 секунд...');
        setTimeout(startSSE, 5000);
    };
}

async function startBooking(seatId) {
    currentHoldSeatId = seatId;

    try {
        showMessage('Бронирование места...', 'info');

        const holdResponse = await fetch('/api/seats/' + seatId + '/hold', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const holdData = await holdResponse.json();

        if (!holdResponse.ok) {
            showMessage(holdData.message || 'Не удалось забронировать место', 'error');
            return;
        }

        startHoldTimer(5);

        const name = prompt('Введите ваше имя:');
        if (!name || name.trim() === '') {
            cancelHold(seatId);
            stopHoldTimer();
            return;
        }

        const phone = prompt('Введите ваш телефон (только цифры, минимум 10):');
        if (!phone || phone.trim() === '') {
            cancelHold(seatId);
            stopHoldTimer();
            return;
        }

        const cleanPhone = phone.replace(/[^0-9]/g, '');
        if (cleanPhone.length < 10) {
            showMessage('Введите корректный номер телефона (минимум 10 цифр)', 'error');
            cancelHold(seatId);
            stopHoldTimer();
            return;
        }

        showMessage('Подтверждение бронирования...', 'info');

        const confirmResponse = await fetch('/api/seats/' + seatId + '/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                customerName: name.trim(),
                customerPhone: cleanPhone
            })
        });

        stopHoldTimer();

        const confirmData = await confirmResponse.json();

        if (confirmResponse.ok) {
            showMessage('Билет №' + confirmData.ticketId + ' успешно оформлен!', 'success');
        } else {
            showMessage(confirmData.message || 'Ошибка при подтверждении', 'error');
        }

    } catch (error) {
        stopHoldTimer();
        console.error('Ошибка:', error);
        showMessage('Ошибка соединения с сервером', 'error');
    } finally {
        currentHoldSeatId = null;
    }
}

function startHoldTimer(minutes) {
    let timeLeft = minutes * 60;

    holdTimer = setInterval(() => {
        const mins = Math.floor(timeLeft / 60);
        const secs = timeLeft % 60;
        showMessage(`Время на бронирование: ${mins}:${secs.toString().padStart(2, '0')}`, 'info');
        timeLeft--;
    }, 1000);

    holdTimeout = setTimeout(() => {
        stopHoldTimer();
        showMessage('Время на бронирование истекло', 'error');
        if (currentHoldSeatId) {
            cancelHold(currentHoldSeatId);
        }
    }, minutes * 60 * 1000);
}

function stopHoldTimer() {
    if (holdTimer) {
        clearInterval(holdTimer);
        holdTimer = null;
    }
    if (holdTimeout) {
        clearTimeout(holdTimeout);
        holdTimeout = null;
    }
}

async function cancelHold(seatId) {
    try {
        await fetch('/api/seats/' + seatId + '/cancel-hold', { method: 'POST' });
        showMessage('Бронирование отменено', 'info');
    } catch (error) {
        console.error('Ошибка отмены:', error);
    }
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
            loadMyBookings();
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
document.getElementById('refreshBtn').onclick = () => {
    showMessage('Схема обновлена', 'info');
};
document.getElementById('myBookingsBtn').onclick = loadMyBookings;

startSSE();
loadStats();
loadMyBookings();