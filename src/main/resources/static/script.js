const API_BASE = '/api';

let seats = [];

async function loadSeats() {
    try {
        const response = await fetch(`${API_BASE}/seats`);
        if (!response.ok) throw new Error('Ошибка загрузки');
        seats = await response.json();
        renderHall();
    } catch (error) {
        showMessage('Ошибка загрузки схемы зала', 'error');
        console.error(error);
    }
}

function renderHall() {
    const hallDiv = document.getElementById('hall');
    hallDiv.innerHTML = '';

    if (!seats || seats.length === 0) {
        hallDiv.innerHTML = '<div class="loading">Загрузка...</div>';
        return;
    }

    let currentRow = 0;
    let rowDiv = null;
    let seatsRow = null;

    seats.forEach(seat => {
        if (seat.row !== currentRow) {
            // Создаем новый ряд
            currentRow = seat.row;
            rowDiv = document.createElement('div');
            rowDiv.className = 'row';

            const titleDiv = document.createElement('div');
            titleDiv.className = 'row-title';
            titleDiv.textContent = `Ряд ${currentRow}`;
            rowDiv.appendChild(titleDiv);

            seatsRow = document.createElement('div');
            seatsRow.className = 'seats-row';
            rowDiv.appendChild(seatsRow);

            hallDiv.appendChild(rowDiv);
        }

        const seatDiv = document.createElement('div');
        seatDiv.className = `seat ${seat.status.toLowerCase()}`;
        seatDiv.textContent = seat.number;

        if (seat.status === 'FREE') {
            seatDiv.onclick = () => bookSeat(seat.id);
        }

        seatsRow.appendChild(seatDiv);
    });
}

async function bookSeat(seatId) {
    const seat = seats.find(s => s.id === seatId);
    if (!seat || seat.status !== 'FREE') {
        showMessage('Это место уже занято', 'error');
        return;
    }

    const name = prompt('Введите ваше имя');
    if (!name || name.trim() === '') {
        showMessage('Имя обязательно для бронирования', 'error');
        return;
    }

    const phone = prompt('Введите ваш телефон');
    if (!phone || phone.trim() === '') {
        showMessage('Телефон обязателен для бронирования', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/seats/${seatId}/book`, {
            method: 'POST',
            headers: {
                'Content-Type': application/json'
            },
            body: JSON.stringify({
                customerName: name.trim(),
                customerPhone: phone.trim()
            })
        });

        const data = await response.json();

        if (response.ok) {
            showMessage(`✅ Билет №${data.ticketId} успешно оформлен!`, 'success');
            loadSeats(); // Обновляем схему
            loadStats();  // Обновляем статистику
        } else {
            showMessage(`❌ ${data.message || 'Место уже занято'}`, 'error');
            loadSeats(); // Обновляем схему, чтобы показать актуальное состояние
        }
    } catch (error) {
        showMessage('Ошибка при бронировании', 'error');
        console.error(error);
    }
}

async function loadStats() {
    try {
        const response = await fetch(`${API_BASE}/stats`);
        if (!response.ok) throw new Error('Ошибка загрузки статистики');
        const stats = await response.json();

        const statsDiv = document.getElementById('stats');
        statsDiv.innerHTML = `
            <strong>📊 Статистика зала</strong><br>
            Всего мест: ${stats.totalSeats}<br>
            Занято мест: ${stats.bookedSeats}<br>
            Свободно мест: ${stats.freeSeats}<br>
            Занятость: ${stats.occupancyPercentage}%
        `;
    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
    }
}

function showMessage(text, type) {
    const messageDiv = document.getElementById('message');
    messageDiv.textContent = text;
    messageDiv.className = `message ${type}`;
    setTimeout(() => {
        messageDiv.textContent = '';
        messageDiv.className = 'message';
    }, 3000);
}

// Загрузка статистики каждые 5 секунд (автообновление)
let statsInterval;

function startStatsAutoRefresh() {
    if (statsInterval) clearInterval(statsInterval);
    statsInterval = setInterval(() => {
        loadStats();
    }, 5000);
}

// Обработчики кнопок
document.getElementById('statsBtn').addEventListener('click', loadStats);
document.getElementById('refreshBtn').addEventListener('click', () => {
    loadSeats();
    loadStats();
    showMessage('Схема обновлена', 'success');
});

// Инициализация
loadSeats();
loadStats();
startStatsAutoRefresh();