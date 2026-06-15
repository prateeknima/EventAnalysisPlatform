import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 1,
    iterations: 20,
    thresholds: {
        checks: ['rate==1.0'],
        http_req_duration: ['p(95)<500'],
    },
};

export default function () {
    const incidentId = `INC-RATE-${__ITER}-${Date.now()}`;

    const payload = JSON.stringify({
        incidentId: incidentId,
        source: 'rate-limit-test',
        severity: 'HIGH',
        message: 'k6 rate limit test',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Correlation-Id': `rate-limit-${__ITER}`,
            Authorization: `Bearer ${__ENV.ACCESS_TOKEN}`,
        },
    };

    const response = http.post('http://localhost:8080/incidents', payload, params);

    check(response, {
        'returns 202 before limit or 429 after limit': (r) =>
            r.status === 202 || r.status === 429,
    });
}