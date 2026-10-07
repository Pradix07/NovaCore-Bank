// Lightweight Pure HTML5 Canvas Chart Renderer (Zero External Dependencies)
const BankCharts = {
  
  // Render Area / Line Chart
  renderLineChart(canvasId, labels, dataPoints, color = '#6366f1') {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = (canvas.width = canvas.parentElement.clientWidth || 400);
    const height = (canvas.height = canvas.parentElement.clientHeight || 250);

    ctx.clearRect(0, 0, width, height);

    if (!dataPoints || dataPoints.length === 0) {
      ctx.fillStyle = '#9ca3af';
      ctx.font = '14px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('No data available', width / 2, height / 2);
      return;
    }

    const padding = { top: 30, right: 30, bottom: 40, left: 60 };
    const chartW = width - padding.left - padding.right;
    const chartH = height - padding.top - padding.bottom;

    const maxVal = Math.max(...dataPoints, 100) * 1.15;
    const minVal = 0;

    // Draw Grid & Y-Axis Labels
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.06)';
    ctx.lineWidth = 1;
    ctx.fillStyle = '#6b7280';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'right';

    const ySteps = 4;
    for (let i = 0; i <= ySteps; i++) {
      const val = minVal + (maxVal - minVal) * (i / ySteps);
      const y = padding.top + chartH - (i / ySteps) * chartH;

      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();

      ctx.fillText('$' + Math.round(val).toLocaleString(), padding.left - 10, y + 4);
    }

    // Map data to canvas coordinates
    const stepX = chartW / Math.max(1, dataPoints.length - 1);
    const points = dataPoints.map((val, idx) => ({
      x: padding.left + idx * stepX,
      y: padding.top + chartH - ((val - minVal) / (maxVal - minVal)) * chartH
    }));

    // Area Gradient
    const gradient = ctx.createLinearGradient(0, padding.top, 0, height - padding.bottom);
    gradient.addColorStop(0, 'rgba(99, 102, 241, 0.45)');
    gradient.addColorStop(1, 'rgba(99, 102, 241, 0.0)');

    // Draw Fill
    ctx.beginPath();
    ctx.moveTo(points[0].x, height - padding.bottom);
    for (let i = 0; i < points.length; i++) {
      ctx.lineTo(points[i].x, points[i].y);
    }
    ctx.lineTo(points[points.length - 1].x, height - padding.bottom);
    ctx.closePath();
    ctx.fillStyle = gradient;
    ctx.fill();

    // Draw Line
    ctx.beginPath();
    ctx.moveTo(points[0].x, points[0].y);
    for (let i = 1; i < points.length; i++) {
      ctx.lineTo(points[i].x, points[i].y);
    }
    ctx.strokeStyle = color;
    ctx.lineWidth = 3;
    ctx.stroke();

    // Draw Dots & X Labels
    ctx.textAlign = 'center';
    points.forEach((p, idx) => {
      ctx.beginPath();
      ctx.arc(p.x, p.y, 5, 0, Math.PI * 2);
      ctx.fillStyle = '#fff';
      ctx.fill();
      ctx.strokeStyle = color;
      ctx.lineWidth = 2;
      ctx.stroke();

      if (labels && labels[idx]) {
        ctx.fillStyle = '#9ca3af';
        ctx.fillText(labels[idx], p.x, height - 15);
      }
    });
  },

  // Render Doughnut / Pie Chart
  renderDoughnutChart(canvasId, labels, dataValues, colors = ['#6366f1', '#10b981', '#06b6d4', '#f59e0b', '#ec4899']) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = (canvas.width = canvas.parentElement.clientWidth || 300);
    const height = (canvas.height = canvas.parentElement.clientHeight || 250);

    ctx.clearRect(0, 0, width, height);

    const total = dataValues.reduce((a, b) => a + b, 0);
    if (total === 0) {
      ctx.fillStyle = '#9ca3af';
      ctx.font = '14px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('No data available', width / 2, height / 2);
      return;
    }

    const centerX = width / 2;
    const centerY = height / 2;
    const outerRadius = Math.min(centerX, centerY) - 20;
    const innerRadius = outerRadius * 0.62;

    let startAngle = -Math.PI / 2;

    dataValues.forEach((val, idx) => {
      const sliceAngle = (val / total) * 2 * Math.PI;
      const endAngle = startAngle + sliceAngle;

      ctx.beginPath();
      ctx.arc(centerX, centerY, outerRadius, startAngle, endAngle);
      ctx.arc(centerX, centerY, innerRadius, endAngle, startAngle, true);
      ctx.closePath();

      ctx.fillStyle = colors[idx % colors.length];
      ctx.fill();

      startAngle = endAngle;
    });

    // Center text
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 16px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(total + ' Txns', centerX, centerY);
  }
};
