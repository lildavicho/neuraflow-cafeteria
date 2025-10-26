/**
 * Algolia Search Module for Frontend
 * Uses Search API Key (read-only) - safe to expose on client
 */

// Algolia configuration - Search API Key only (read-only)
const ALGOLIA_CONFIG = {
  applicationId: 'ZC1H8MVX05',
  searchApiKey: '4dea24d6f6c0f12ac5e8d9b4e2f890c0', // Search API Key (read-only)
  indexName: 'productos_venta'
};

// Algolia Search Client (using CDN)
let searchClient = null;
let searchIndex = null;

/**
 * Initialize Algolia search client
 * @returns {Promise<void>}
 */
export async function initAlgoliaSearch() {
  try {
    // Load Algolia Search library from CDN if not already loaded
    if (!window.algoliasearch) {
      await loadAlgoliaScript();
    }

    // Initialize search client
    searchClient = window.algoliasearch(
      ALGOLIA_CONFIG.applicationId,
      ALGOLIA_CONFIG.searchApiKey
    );

    // Get search index
    searchIndex = searchClient.initIndex(ALGOLIA_CONFIG.indexName);

    console.log('[Algolia] Search client initialized successfully');
    return true;
  } catch (error) {
    console.error('[Algolia] Failed to initialize:', error);
    return false;
  }
}

/**
 * Load Algolia Search library from CDN
 * @private
 */
function loadAlgoliaScript() {
  return new Promise((resolve, reject) => {
    if (window.algoliasearch) {
      resolve();
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/algoliasearch@4.22.0/dist/algoliasearch-lite.umd.js';
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

/**
 * Search products using Algolia
 * @param {string} query - Search query
 * @param {Object} options - Search options
 * @returns {Promise<Array>} Search results
 */
export async function searchProducts(query, options = {}) {
  try {
    // Initialize if not already done
    if (!searchIndex) {
      const initialized = await initAlgoliaSearch();
      if (!initialized) {
        throw new Error('Algolia search not initialized');
      }
    }

    const {
      hitsPerPage = 20,
      page = 0,
      filters = '',
      facets = []
    } = options;

    const searchParams = {
      query,
      hitsPerPage,
      page,
      filters,
      facets
    };

    const result = await searchIndex.search(query, searchParams);

    console.log(`[Algolia] Search for "${query}" returned ${result.hits.length} results`);

    return {
      hits: result.hits,
      nbHits: result.nbHits,
      page: result.page,
      nbPages: result.nbPages,
      processingTimeMS: result.processingTimeMS
    };
  } catch (error) {
    console.error('[Algolia] Search error:', error);
    throw error;
  }
}

/**
 * Search products with facets (category filtering)
 * @param {string} query - Search query
 * @param {string} category - Category filter
 * @returns {Promise<Array>} Search results
 */
export async function searchProductsByCategory(query, category) {
  const filters = category ? `category:"${category}"` : '';
  return searchProducts(query, { filters });
}

/**
 * Get product suggestions (autocomplete)
 * @param {string} query - Partial search query
 * @param {number} limit - Max number of suggestions
 * @returns {Promise<Array>} Suggestions
 */
export async function getProductSuggestions(query, limit = 5) {
  try {
    const result = await searchProducts(query, { hitsPerPage: limit });
    return result.hits.map(hit => ({
      id: hit.objectID,
      name: hit.name,
      code: hit.code,
      price: hit.price,
      category: hit.category
    }));
  } catch (error) {
    console.error('[Algolia] Suggestions error:', error);
    return [];
  }
}

/**
 * Search products with price range filter
 * @param {string} query - Search query
 * @param {number} minPrice - Minimum price
 * @param {number} maxPrice - Maximum price
 * @returns {Promise<Array>} Search results
 */
export async function searchProductsByPriceRange(query, minPrice, maxPrice) {
  const filters = `price >= ${minPrice} AND price <= ${maxPrice}`;
  return searchProducts(query, { filters });
}

/**
 * Get all available categories from search results
 * @returns {Promise<Array>} Categories
 */
export async function getCategories() {
  try {
    if (!searchIndex) {
      await initAlgoliaSearch();
    }

    const result = await searchIndex.search('', {
      hitsPerPage: 0,
      facets: ['category']
    });

    return Object.keys(result.facets?.category || {});
  } catch (error) {
    console.error('[Algolia] Get categories error:', error);
    return [];
  }
}

/**
 * Clear search cache
 */
export function clearSearchCache() {
  if (searchClient && searchClient.clearCache) {
    searchClient.clearCache();
    console.log('[Algolia] Cache cleared');
  }
}

/**
 * Check if Algolia is initialized
 * @returns {boolean}
 */
export function isAlgoliaInitialized() {
  return searchIndex !== null;
}

// Auto-initialize on module load
initAlgoliaSearch().catch(err => {
  console.warn('[Algolia] Auto-initialization failed:', err);
});

export default {
  initAlgoliaSearch,
  searchProducts,
  searchProductsByCategory,
  getProductSuggestions,
  searchProductsByPriceRange,
  getCategories,
  clearSearchCache,
  isAlgoliaInitialized
};
