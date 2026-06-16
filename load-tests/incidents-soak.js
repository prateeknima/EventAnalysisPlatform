import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 10,
    duration: '10m',
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<750'],
    },
};

export default function () {
    const incidentId = `INC-SOAK-${__VU}-${__ITER}-${Date.now()}`;

    const payload = JSON.stringify({
        incidentId: incidentId,
        source: `soak-test-${__VU}-${__ITER}`,
        severity: 'HIGH',
        message: 'k6 soak load test',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Correlation-Id': `soak-test-${__VU}-${__ITER}`,
            Authorization: `Bearer ${__ENV.ACCESS_TOKEN}`,
        },
    };

    const response = http.post('http://localhost:8080/incidents', payload, params);

    check(response, {
        'POST /incidents returns 202': (r) => r.status === 202,
    });

    sleep(1);
}