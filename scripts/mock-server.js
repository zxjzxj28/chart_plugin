/**
 * Mock A11y API Server for testing
 *
 * Usage:
 *   node mock-server.js
 *
 * The server will run on http://localhost:3000
 *
 * Configure the plugin to use this endpoint:
 *   chartA11y {
 *       apiEndpoint = 'http://localhost:3000/api/v1/chart-a11y'
 *   }
 */

const http = require('http');

const PORT = 3000;

function generateDescriptions(chart, locales) {
    const descriptions = {};

    for (const locale of locales) {
        const isZh = locale.startsWith('zh');
        const chartType = chart.type || 'CUSTOM';
        const title = chart.title || 'Chart';
        const labels = chart.data?.labels || [];
        const datasets = chart.data?.datasets || [];

        const dataset = datasets[0] || { name: '', values: [], unit: '' };
        const values = dataset.values || [];
        const unit = dataset.unit || '';

        // Generate brief description
        let brief, detailed;
        const dataPoints = [];

        if (isZh) {
            const typeNames = {
                'BAR': '柱状图',
                'LINE': '折线图',
                'PIE': '饼图',
                'RADAR': '雷达图',
                'SCATTER': '散点图',
                'CUSTOM': '图表'
            };
            const typeName = typeNames[chartType] || '图表';

            brief = `${typeName}展示${title}`;
            detailed = `这是一个${typeName}，标题为"${title}"。`;

            if (values.length > 0) {
                const max = Math.max(...values);
                const maxIndex = values.indexOf(max);
                detailed += `数据包含${values.length}个数据点，最高值为${max}${unit}（${labels[maxIndex] || `第${maxIndex + 1}项`}）。`;
            }

            // Generate data points
            for (let i = 0; i < labels.length && i < values.length; i++) {
                dataPoints.push(`${labels[i]}，${dataset.name || '数值'}为${values[i]}${unit}`);
            }
        } else {
            const typeNames = {
                'BAR': 'bar chart',
                'LINE': 'line chart',
                'PIE': 'pie chart',
                'RADAR': 'radar chart',
                'SCATTER': 'scatter chart',
                'CUSTOM': 'chart'
            };
            const typeName = typeNames[chartType] || 'chart';

            brief = `${typeName} showing ${title}`;
            detailed = `This is a ${typeName} titled "${title}". `;

            if (values.length > 0) {
                const max = Math.max(...values);
                const maxIndex = values.indexOf(max);
                detailed += `Contains ${values.length} data points with maximum value ${max}${unit} at ${labels[maxIndex] || `position ${maxIndex + 1}`}.`;
            }

            // Generate data points
            for (let i = 0; i < labels.length && i < values.length; i++) {
                dataPoints.push(`${labels[i]}: ${values[i]}${unit}`);
            }
        }

        descriptions[locale] = { brief, detailed, dataPoints };
    }

    return descriptions;
}

const server = http.createServer((req, res) => {
    console.log(`${new Date().toISOString()} ${req.method} ${req.url}`);

    // CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }

    if (req.method === 'POST' && req.url === '/api/v1/chart-a11y') {
        let body = '';

        req.on('data', chunk => {
            body += chunk.toString();
        });

        req.on('end', () => {
            try {
                const request = JSON.parse(body);
                const chart = request.chart || {};
                const locales = request.locales || ['en'];

                console.log(`  Processing chart: ${chart.id} (${chart.type})`);

                const response = {
                    descriptions: generateDescriptions(chart, locales)
                };

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(response, null, 2));

            } catch (error) {
                console.error('  Error:', error.message);
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Invalid request' }));
            }
        });
    } else {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Not found' }));
    }
});

server.listen(PORT, () => {
    console.log(`Mock A11y API Server running on http://localhost:${PORT}`);
    console.log(`Endpoint: http://localhost:${PORT}/api/v1/chart-a11y`);
    console.log('');
    console.log('Press Ctrl+C to stop');
});
