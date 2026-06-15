import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 5,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<400'],
    },
};

export default function () {
    const params = {
        headers: {
            Authorization: `Bearer ${__ENV.ACCESS_TOKEN}`,
        },
    };

    const searchResponse = http.get(
        'http://localhost:8080/incidents/search?q=payment',
        params
    );

    check(searchResponse, {
        'GET /incidents/search returns 200': (r) => r.status === 200,
    });

    sleep(1);
}