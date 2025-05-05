<template>
  <div class="container">
    <h1>Schulverwaltung</h1>
    
    <!-- Lehrer-Sektion -->
    <div class="section">
      <h2>Lehrer hinzufügen</h2>
      <div class="form">
        <input 
          type="text" 
          v-model="newLehrer.name" 
          placeholder="Name des Lehrers" 
        />
        <input 
          type="text" 
          v-model="newLehrer.fachgebiet" 
          placeholder="Fachgebiet" 
        />
        <button @click="addLehrer">Lehrer hinzufügen</button>
      </div>
      
      <h2>Lehrerliste</h2>
      <div v-if="lehrerLoading">Laden...</div>
      <div v-else-if="lehrerError">{{ lehrerError }}</div>
      <div v-else-if="lehrer.length === 0">Keine Lehrer gefunden</div>
      <table v-else>
        <tr>
          <th>Name</th>
          <th>Fachgebiet</th>
          <th>Aktion</th>
        </tr>
        <tr v-for="l in lehrer" :key="l.id">
          <td>{{ l.name }}</td>
          <td>{{ l.fachgebiet }}</td>
          <td>
            <button @click="loadSchuelerForLehrer(l.id)">Schüler anzeigen</button>
          </td>
        </tr>
      </table>
    </div>
    
    <!-- Schüler-Sektion -->
    <div class="section">
      <h2>Schüler hinzufügen</h2>
      <div class="form">
        <input 
          type="text" 
          v-model="newSchueler.name" 
          placeholder="Name des Schülers" 
        />
        <input 
          type="text" 
          v-model="newSchueler.klasse" 
          placeholder="Klasse" 
        />
        <select v-model="newSchueler.lehrerId">
          <option value="">Lehrer auswählen</option>
          <option v-for="l in lehrer" :key="l.id" :value="l.id">{{ l.name }}</option>
        </select>
        <button @click="addSchueler">Schüler hinzufügen</button>
      </div>
      
      <h2>Schülerliste {{ selectedLehrerName ? 'von ' + selectedLehrerName : '' }}</h2>
      <div v-if="schuelerLoading">Laden...</div>
      <div v-else-if="schuelerError">{{ schuelerError }}</div>
      <div v-else-if="schueler.length === 0">Keine Schüler gefunden</div>
      <table v-else>
        <tr>
          <th>Name</th>
          <th>Klasse</th>
        </tr>
        <tr v-for="s in schueler" :key="s.id">
          <td>{{ s.name }}</td>
          <td>{{ s.klasse }}</td>
        </tr>
      </table>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'

export default {
  setup() {
    // Lehrer-States
    const lehrer = ref([])
    const lehrerLoading = ref(false)
    const lehrerError = ref(null)
    const newLehrer = ref({ name: '', fachgebiet: '' })
    
    // Schüler-States
    const schueler = ref([])
    const schuelerLoading = ref(false)
    const schuelerError = ref(null)
    const newSchueler = ref({ name: '', klasse: '', lehrerId: '' })
    const selectedLehrerName = ref('')
    
    // Lehrer laden
    const loadLehrer = async () => {
      lehrerLoading.value = true
      try {
        const response = await axios.get('/api/lehrer')
        lehrer.value = response.data
      } catch (err) {
        lehrerError.value = 'Fehler beim Laden der Lehrer'
      } finally {
        lehrerLoading.value = false
      }
    }
    
    // Schüler für alle laden
    const loadAllSchueler = async () => {
      schuelerLoading.value = true
      selectedLehrerName.value = ''
      try {
        const response = await axios.get('/api/schueler')
        schueler.value = response.data
      } catch (err) {
        schuelerError.value = 'Fehler beim Laden der Schüler'
      } finally {
        schuelerLoading.value = false
      }
    }
    
    // Schüler für bestimmten Lehrer laden
    const loadSchuelerForLehrer = async (lehrerId) => {
      schuelerLoading.value = true
      try {
        const lehrerResponse = await axios.get(`/api/lehrer/${lehrerId}`)
        selectedLehrerName.value = lehrerResponse.data.name
        
        const response = await axios.get(`/api/schueler/lehrer/${lehrerId}`)
        schueler.value = response.data
      } catch (err) {
        schuelerError.value = 'Fehler beim Laden der Schüler'
      } finally {
        schuelerLoading.value = false
      }
    }
    
    // Lehrer hinzufügen
    const addLehrer = async () => {
      if (!newLehrer.value.name) return
      
      lehrerLoading.value = true
      try {
        await axios.post('/api/lehrer', newLehrer.value)
        newLehrer.value = { name: '', fachgebiet: '' }
        await loadLehrer()
      } catch (err) {
        lehrerError.value = 'Fehler beim Hinzufügen des Lehrers'
      } finally {
        lehrerLoading.value = false
      }
    }
    
    // Schüler hinzufügen
    const addSchueler = async () => {
      if (!newSchueler.value.name || !newSchueler.value.lehrerId) return
      
      schuelerLoading.value = true
      try {
        await axios.post('/api/schueler', newSchueler.value)
        newSchueler.value = { name: '', klasse: '', lehrerId: '' }
        await loadAllSchueler()
      } catch (err) {
        schuelerError.value = 'Fehler beim Hinzufügen des Schülers'
      } finally {
        schuelerLoading.value = false
      }
    }

    onMounted(() => {
      loadLehrer()
      loadAllSchueler()
    })

    return {
      // Lehrer
      lehrer,
      lehrerLoading,
      lehrerError,
      newLehrer,
      addLehrer,
      
      // Schüler
      schueler,
      schuelerLoading,
      schuelerError,
      newSchueler,
      addSchueler,
      loadSchuelerForLehrer,
      selectedLehrerName
    }
  }
}
</script>

<style>
/*
.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

h1, h2 {
  color: #333;
}

.section {
  margin-bottom: 30px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 5px;
}

.form {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

input, select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  flex-grow: 1;
}

button {
  padding: 8px 15px;
  background: #4285f4;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

th, td {
  padding: 8px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

th {
  background-color: #f2f2f2;
}
  */
</style>