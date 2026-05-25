let seats = [];
let updateInterval = null;
let currentHoldSeatId = null;

async function loadSeats() {
    try {
        const res = await fetch('/api/seats');
        seats = await res.json();
        renderHall();
    } catch (error) {
        console.error('Ошибка загрузки:', error);
    }
}

function renderHall() {
    const hallDiv = document.getElementById('hall');
    hallDiv.innerHTML = '';

    if (!seats.length) {
        hallDiv.innerHTML = '<div class="loading">Нет мест</div>';
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
            seatDiv.style.cursor = 'wait';
            seatDiv.title = 'Временно забронировано другим пользователем';
        } else {
            seatDiv.style.cursor = 'not-allowed';
            seatDiv.title = 'Уже занято';
        }

        seatsRow.appendChild(seatDiv);
    }
}

async function startBooking(seatId) {
    try {
        // Шаг 1: Временное резервирование
        const holdRes = await fetch('/api/seats/' + seatId + '/hold', { method: 'POST' });
        const holdData = await holdRes.json();

        if (!holdRes.ok) {
            showMessage(holdData.message || 'Место уже занято', 'error');
            loadSeats();
            return;
        }

        showMessage(holdData.message, 'info');
        currentHoldSeatId = seatId;

        // Шаг 2: Запрос данных пользователя
        const name = prompt('Ваше имя:');
        if (!name) {
            cancelHold(seatId);
            return;
        }

        const phone = prompt('Ваш телефон:');
        if (!phone) {
            cancelHold(seatId);
            return;
        }

        // Шаг 3: Подтверждение бронирования
        const confirmRes = await fetch('/api/seats/' + seatId + '/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ customerName: name, customerPhone: phone })
        });

        const confirmData = await confirmRes.json();

        if (confirmRes.ok) {
            showMessage('✅ ' + confirmData.message + ' Билет №' + confirmData.ticketId, 'success');
            currentHoldSeatId = null;
            loadSeats();
            loadStats();
        } else {
            showMessage('❌ ' + confirmData.message, 'error');
            loadSeats();
        }

    } catch (e) {
        showMessage('Ошибка соединения', 'error');
    }
}

async function cancelHold(seatId) {
    try {
        await fetch('/api/seats/' + seatId + '/cancel-hold', { method: 'POST' });
        showMessage('Бронирование отменено', 'info');
        loadSeats();
    } catch (e) {
        console.error('Ошибка отмены:', e);
    }
}

// Автообновление каждые 3 секунды
function startAutoRefresh() {
    if (updateInterval) clearInterval(updateInterval);
    updateInterval = setInterval(() => {
        loadSeats();
        loadStats();
    }, 3000);
}

// ... остальные функции (loadStats, showMessage, обработчики кнопок)

startAutoRefresh();
loadSeats();
loadStats();