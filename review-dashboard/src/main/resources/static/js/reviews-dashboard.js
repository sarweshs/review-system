// Shared JavaScript for Reviews Dashboard
// Used by both admin.html and user.html templates

let currentTab = 'good';
let currentPage = 0;
let pageSize = 20;
let totalPages = 0;
let totalItems = 0;
let platforms = [];

// API base URL - update this to match your service port
const API_BASE_URL = window.API_BASE_URL || 'http://localhost:7070';

// Initialize the dashboard
function initializeReviewsDashboard() {
    console.log('Initializing reviews dashboard...');
    loadSummary();
    loadPlatforms();
    pageSize = parseInt(document.getElementById('pageSizeFilter').value);
    loadGoodReviews();
    loadBadReviews();
    
    // Ensure functions are globally available
    window.applyFilters = applyFilters;
    window.switchTab = switchTab;
    window.refreshData = refreshData;
    window.changePageSize = changePageSize;
    window.changePageSizeFromPagination = changePageSizeFromPagination;
    window.toggleComment = toggleComment;
    window.toggleBadComment = toggleBadComment;
    window.viewReviewDetails = viewReviewDetails;
    window.viewBadReviewDetails = viewBadReviewDetails;
    
    console.log('Reviews dashboard initialized successfully');
}

function loadPlatforms() {
    fetch(`${API_BASE_URL}/api/reviews/statistics`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(stats => {
            platforms = stats.platforms || [];
            const platformFilter = document.getElementById('platformFilter');
            if (platformFilter) {
                platformFilter.innerHTML = '<option value="">All Platforms</option>';
                platforms.forEach(p => {
                    const opt = document.createElement('option');
                    opt.value = p;
                    opt.textContent = p;
                    platformFilter.appendChild(opt);
                });
            }
        })
        .catch(error => {
            console.error('Error loading platforms:', error);
            console.error('Make sure your service is running on port 7070 and CORS is properly configured');
        });
}

function switchTab(tab) {
    currentTab = tab;
    
    // Update tab styling
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    
    event.target.classList.add('active');
    document.getElementById(tab + '-reviews').classList.add('active');
}

function loadSummary() {
    fetch(`${API_BASE_URL}/api/reviews/summary`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            const totalGoodElement = document.getElementById('totalGoodReviews');
            const totalBadElement = document.getElementById('totalBadReviews');
            const totalElement = document.getElementById('totalReviews');
            const avgElement = document.getElementById('avgRating');
            
            if (totalGoodElement) totalGoodElement.textContent = data.totalGoodReviews || 0;
            if (totalBadElement) totalBadElement.textContent = data.totalBadReviews || 0;
            if (totalElement) totalElement.textContent = (data.totalGoodReviews || 0) + (data.totalBadReviews || 0);
            
            // Load average rating
            fetch(`${API_BASE_URL}/api/reviews/statistics`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(stats => {
                    if (avgElement) avgElement.textContent = (stats.averageRating || 0).toFixed(1);
                })
                .catch(error => {
                    console.error('Error loading statistics:', error);
                    if (avgElement) avgElement.textContent = 'N/A';
                });
        })
        .catch(error => {
            console.error('Error loading summary:', error);
            console.error('Make sure your service is running on port 7070 and CORS is properly configured');
        });
}

function loadGoodReviews() {
    const loading = document.getElementById('good-reviews-loading');
    const error = document.getElementById('good-reviews-error');
    const table = document.getElementById('good-reviews-table');
    const pagination = document.getElementById('good-reviews-pagination');
    
    if (loading) loading.style.display = 'block';
    if (error) error.style.display = 'none';
    if (table) table.style.display = 'none';
    if (pagination) pagination.style.display = 'none';
    
    const platformFilter = document.getElementById('platformFilter');
    const ratingFilter = document.getElementById('ratingFilter');
    const searchFilter = document.getElementById('searchFilter');
    
    const platform = platformFilter ? platformFilter.value : '';
    const rating = ratingFilter ? ratingFilter.value : '';
    const search = searchFilter ? searchFilter.value : '';
    
    let url = `${API_BASE_URL}/api/reviews?page=${currentPage}&size=${pageSize}`;
    
    if (platform) url += `&platform=${encodeURIComponent(platform)}`;
    if (rating) {
        const [min, max] = rating.split('-');
        url += `&minRating=${min}&maxRating=${max}`;
    }
    if (search) url += `&search=${encodeURIComponent(search)}`;
    
    console.log('Loading good reviews from URL:', url);
    
    fetch(url)
        .then(response => {
            console.log('Response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Received data:', data);
            if (loading) loading.style.display = 'none';
            
            if (data.reviews && data.reviews.length > 0) {
                displayGoodReviews(data.reviews);
                setupPagination(data);
                if (table) table.style.display = 'table';
                if (pagination) pagination.style.display = 'flex';
            } else {
                const tbody = document.getElementById('good-reviews-tbody');
                if (tbody) {
                    tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 40px;">No good reviews found</td></tr>';
                }
                if (table) table.style.display = 'table';
            }
        })
        .catch(error => {
            console.error('Error loading good reviews:', error);
            if (loading) loading.style.display = 'none';
            if (error) {
                error.style.display = 'block';
                error.textContent = 'Error loading good reviews: ' + error.message;
            }
        });
}

function loadBadReviews() {
    const loading = document.getElementById('bad-reviews-loading');
    const error = document.getElementById('bad-reviews-error');
    const table = document.getElementById('bad-reviews-table');
    
    if (loading) loading.style.display = 'block';
    if (error) error.style.display = 'none';
    if (table) table.style.display = 'none';
    
    fetch(`${API_BASE_URL}/api/bad-review-records`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (loading) loading.style.display = 'none';
            
            if (data && data.length > 0) {
                displayBadReviews(data);
                if (table) table.style.display = 'table';
            } else {
                const tbody = document.getElementById('bad-reviews-tbody');
                if (tbody) {
                    tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 40px;">No bad reviews found</td></tr>';
                }
                if (table) table.style.display = 'table';
            }
        })
        .catch(error => {
            console.error('Error loading bad reviews:', error);
            if (loading) loading.style.display = 'none';
            if (error) {
                error.style.display = 'block';
                error.textContent = 'Error loading bad reviews: ' + error.message;
            }
        });
}

function displayGoodReviews(reviews) {
    const tbody = document.getElementById('good-reviews-tbody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    reviews.forEach(review => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${review.id.reviewId}</td>
            <td>${review.entityId}</td>
            <td><span class="platform-badge platform-${review.platform?.toLowerCase()}">${review.platform || 'N/A'}</span></td>
            <td><span class="rating ${getRatingClass(review.rating)}">${review.rating || 'N/A'}</span></td>
            <td>${review.reviewTitle || 'N/A'}</td>
            <td>
                <div class="review-comment" id="comment-${review.reviewId}">
                    ${review.reviewComments || 'No comment'}
                </div>
                ${review.reviewComments && review.reviewComments.length > 100 ? 
                    `<span class="expand-btn" onclick="toggleComment(${review.reviewId})">Show more</span>` : ''}
            </td>
            <td>${formatDate(review.reviewDate)}</td>
            <td>
                <button class="btn btn-secondary" onclick="viewReviewDetails(${review.id.reviewId})">View</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function displayBadReviews(reviews) {
    const tbody = document.getElementById('bad-reviews-tbody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    reviews.forEach(review => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${review.id.reviewId}</td>
            <td><span class="platform-badge platform-${review.platform?.toLowerCase()}">${review.platform || 'N/A'}</span></td>
            <td><span class="rating poor">${review.reason}</span></td>
            <td>
                <div class="review-comment" id="bad-comment-${review.id}">
                    ${truncateJson(review.jsonData)}
                </div>
                <span class="expand-btn" onclick="toggleBadComment(${review.id})">Show more</span>
            </td>
            <td>${formatDate(review.createdDate)}</td>
            <td>
                <button class="btn btn-secondary" onclick="viewBadReviewDetails(${review.id})">View</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function setupPagination(data) {
    currentPage = data.currentPage;
    totalPages = data.totalPages;
    totalItems = data.totalItems;
    
    const pagination = document.getElementById('good-reviews-pagination');
    if (!pagination) return;
    
    pagination.innerHTML = '';
    
    // Previous button
    const prevBtn = document.createElement('button');
    prevBtn.textContent = 'Previous';
    prevBtn.disabled = !data.hasPrevious;
    prevBtn.onclick = () => {
        if (currentPage > 0) {
            currentPage--;
            loadGoodReviews();
        }
    };
    pagination.appendChild(prevBtn);
    
    // Page numbers
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.textContent = i + 1;
        pageBtn.className = i === currentPage ? 'current-page' : '';
        pageBtn.onclick = () => {
            currentPage = i;
            loadGoodReviews();
        };
        pagination.appendChild(pageBtn);
    }
    
    // Next button
    const nextBtn = document.createElement('button');
    nextBtn.textContent = 'Next';
    nextBtn.disabled = !data.hasNext;
    nextBtn.onclick = () => {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadGoodReviews();
        }
    };
    pagination.appendChild(nextBtn);
    
    // Page size dropdown and info
    const pageSizeGroup = document.createElement('span');
    pageSizeGroup.className = 'page-size-group';
    pageSizeGroup.innerHTML = `Page Size: <select id="pageSizeFilter2" onchange="changePageSizeFromPagination()">
        <option value="10" ${pageSize===10?'selected':''}>10</option>
        <option value="20" ${pageSize===20?'selected':''}>20</option>
        <option value="50" ${pageSize===50?'selected':''}>50</option>
        <option value="100" ${pageSize===100?'selected':''}>100</option>
    </select>`;
    pagination.appendChild(pageSizeGroup);
    
    const pageInfo = document.createElement('span');
    pageInfo.className = 'page-info';
    pageInfo.textContent = `Page ${currentPage+1} of ${totalPages} | Total: ${totalItems}`;
    pagination.appendChild(pageInfo);
}

function applyFilters() {
    console.log('Applying filters...');
    console.log('Current tab:', currentTab);
    console.log('applyFilters function is defined:', typeof applyFilters);
    currentPage = 0;
    
    if (currentTab === 'good') {
        console.log('Loading good reviews with filters');
        loadGoodReviews();
    } else {
        console.log('Loading bad reviews with filters');
        loadBadReviews();
    }
}

function refreshData() {
    loadSummary();
    if (currentTab === 'good') {
        loadGoodReviews();
    } else {
        loadBadReviews();
    }
}

function getRatingClass(rating) {
    if (!rating) return 'fair';
    if (rating >= 9) return 'excellent';
    if (rating >= 7) return 'good';
    if (rating >= 5) return 'fair';
    return 'poor';
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        return new Date(dateString).toLocaleDateString();
    } catch (e) {
        return dateString;
    }
}

function truncateJson(jsonString) {
    if (!jsonString) return 'N/A';
    return jsonString.length > 100 ? jsonString.substring(0, 100) + '...' : jsonString;
}

function toggleComment(reviewId) {
    const comment = document.getElementById(`comment-${reviewId}`);
    if (!comment) return;
    
    const expandBtn = comment.nextElementSibling;
    if (!expandBtn) return;
    
    if (comment.classList.contains('expanded')) {
        comment.classList.remove('expanded');
        expandBtn.textContent = 'Show more';
    } else {
        comment.classList.add('expanded');
        expandBtn.textContent = 'Show less';
    }
}

function toggleBadComment(reviewId) {
    const comment = document.getElementById(`bad-comment-${reviewId}`);
    if (!comment) return;
    
    const expandBtn = comment.nextElementSibling;
    if (!expandBtn) return;
    
    if (comment.classList.contains('expanded')) {
        comment.classList.remove('expanded');
        expandBtn.textContent = 'Show more';
    } else {
        comment.classList.add('expanded');
        expandBtn.textContent = 'Show less';
    }
}

function viewReviewDetails(reviewId) {
    // TODO: Implement review details modal
    alert('View details for review ID: ' + reviewId);
}

function viewBadReviewDetails(reviewId) {
    // TODO: Implement bad review details modal
    alert('View details for bad review ID: ' + reviewId);
}

function changePageSize() {
    const pageSizeFilter = document.getElementById('pageSizeFilter');
    if (pageSizeFilter) {
        pageSize = parseInt(pageSizeFilter.value);
        currentPage = 0;
        loadGoodReviews();
    }
}

function changePageSizeFromPagination() {
    const pageSizeFilter2 = document.getElementById('pageSizeFilter2');
    const pageSizeFilter = document.getElementById('pageSizeFilter');
    
    if (pageSizeFilter2 && pageSizeFilter) {
        pageSize = parseInt(pageSizeFilter2.value);
        pageSizeFilter.value = pageSize;
        currentPage = 0;
        loadGoodReviews();
    }
}

// Auto-initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, checking for reviews dashboard elements...');
    
    // Check if we're on a page with reviews dashboard
    const reviewsContainer = document.querySelector('.container, #see-reviews .container');
    if (reviewsContainer) {
        console.log('Reviews dashboard detected, initializing...');
        initializeReviewsDashboard();
    }
}); 