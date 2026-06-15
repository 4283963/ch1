import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== undefined && res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data !== undefined ? res.data : res
  },
  error => {
    return Promise.reject(error)
  }
)

export const getSpaces = () => {
  return request.get('/spaces')
}

export const recognizePlate = (plateNumber) => {
  return request.post('/plate/recognize', { plateNumber })
}

export const getWhiteList = () => {
  return request.get('/plate/whitelist/detail')
}

export const addWhiteList = (data) => {
  return request.post('/plate/whitelist', data)
}

export const deleteWhiteList = (id) => {
  return request.delete(`/plate/whitelist/${id}`)
}

export const getOwners = () => {
  return request.get('/plate/owners')
}

export const addOwner = (data) => {
  return request.post('/plate/owners', data)
}

export const anchorUp = (spaceId) => {
  return request.post('/plate/anchor/up', { spaceId })
}

export const anchorDown = (spaceId) => {
  return request.post('/plate/anchor/down', { spaceId })
}

export const updateSpaceStatusApi = (spaceId, status) => {
  return request.put(`/spaces/${spaceId}/status`, { status })
}

export const getRecords = () => {
  return request.get('/access-records')
}

export const getRecordsByPlate = (plateNumber) => {
  return request.get(`/access-records/plate/${plateNumber}`)
}

export default request
