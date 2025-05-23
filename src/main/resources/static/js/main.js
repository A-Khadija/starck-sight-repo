/**
 * StackSight Main JavaScript
 */

// Form Validation
document.addEventListener('DOMContentLoaded', function() {
    // Get all forms that need validation
    const forms = document.querySelectorAll('.needs-validation');
    
    // Loop over them and prevent submission if invalid
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            
            form.classList.add('was-validated');
        }, false);
    });
    
    // Password visibility toggle
    const togglePasswordButtons = document.querySelectorAll('.toggle-password');
    
    togglePasswordButtons.forEach(button => {
        button.addEventListener('click', function() {
            const input = document.querySelector(this.getAttribute('data-target'));
            
            // Toggle the type attribute
            const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
            input.setAttribute('type', type);
            
            // Toggle the icon
            this.innerHTML = type === 'password' ? 
                '<i class="bi bi-eye"></i>' : 
                '<i class="bi bi-eye-slash"></i>';
        });
    });
});

// Search functionality
const searchForm = document.querySelector('.search-form');
if (searchForm) {
    searchForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const query = document.querySelector('.search-input').value;
        
        // Collect all active filters
        const activeFilters = {
            tags: getActiveFilterValues('tag'),
            date: getActiveFilterValues('date')[0],
            status: getActiveFilterValues('status')[0],
            language: getActiveFilterValues('language')[0]
        };
        
        // For demo purpose, just log the search params
        console.log('Search query:', query);
        console.log('Active filters:', activeFilters);
        
        // In real implementation, this would call your back-end endpoint
        // window.location.href = `/search?q=${encodeURIComponent(query)}&...other params`;
    });
}

// Helper function to get active filter values
function getActiveFilterValues(filterType) {
    const activeFilters = document.querySelectorAll(`.filter-chip.active[data-type="${filterType}"]`);
    return Array.from(activeFilters).map(el => el.dataset.value);
}

// Filter chip toggle
const filterChips = document.querySelectorAll('.filter-chip');
if (filterChips) {
    filterChips.forEach(chip => {
        chip.addEventListener('click', function() {
            // For single-select filter types, deactivate other chips of same type
            const filterType = this.dataset.type;
            const isSingleSelect = ['date', 'status', 'language'].includes(filterType);
            
            if (isSingleSelect) {
                document.querySelectorAll(`.filter-chip[data-type="${filterType}"]`)
                    .forEach(el => el.classList.remove('active'));
            }
            
            // Toggle current chip
            this.classList.toggle('active');
        });
    });
}