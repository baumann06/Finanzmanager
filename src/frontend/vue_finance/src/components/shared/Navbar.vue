<template>
  <nav class="bg-blue-500 text-white px-4 py-2">
    <div class="container mx-auto flex justify-between items-center">
      <div class="text-lg font-bold">Vue Finance</div>
      <button
          class="md:hidden text-white focus:outline-none"
          @click="isOpen = !isOpen"
      >
        <svg
            class="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
        >
          <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 6h16M4 12h16m-7 6h7"
          ></path>
        </svg>
      </button>
      <ul
          class="hidden md:flex space-x-4"
          :class="{ 'block': isOpen, 'hidden': !isOpen }"
      >
        <li v-for="link in links" :key="link.name">
          <a
              :href="link.href"
              class="hover:bg-blue-600 px-3 py-2 rounded"
          >
            {{ link.name }}
          </a>
        </li>
      </ul>
    </div>
  </nav>
</template>

<script>
export default {
  name: 'Navbar',
  data() {
    return {
      isOpen: false,
      links: [
        { name: 'Home', href: '/' },
        { name: 'Finance', href: '/finance' },
        { name: 'Crypto', href: '/crypto' },
        { name: 'About', href: '/about' }
      ]
    };
  }
};
</script>

<style scoped>
/* Add any additional styling here */
</style>