<template>
  <div>
    <h3 class="text-lg font-bold">Notizen</h3>
    <ul>
      <li v-for="note in notes" :key="note.id" class="mb-2">
        <p>{{ note.text }}</p>
        <button @click="$emit('delete-note', note.id)" class="text-red-500">Löschen</button>
      </li>
    </ul>
    <div class="mt-4">
      <textarea v-model="newNote" placeholder="Neue Notiz hinzufügen" class="w-full border rounded-lg p-2"></textarea>
      <button @click="addNote" class="mt-2 bg-blue-500 text-white px-4 py-2 rounded-lg">Hinzufügen</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotesList',
  props: {
    notes: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      newNote: ''
    };
  },
  methods: {
    addNote() {
      if (this.newNote.trim()) {
        this.$emit('add-note', this.newNote.trim());
        this.newNote = '';
      }
    }
  }
};
</script>