import { useState, useEffect, type FormEvent } from 'react'
import api from '../api'
import { useAuthStore } from '../stores/authStore'
import type { User } from '../types'

export default function ProfilePage() {
  const { setAuth, token } = useAuthStore()
  const [profile, setProfile] = useState<User | null>(null)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [currentPw, setCurrentPw] = useState('')
  const [newPw, setNewPw] = useState('')
  const [msg, setMsg] = useState('')
  const [pwMsg, setPwMsg] = useState('')

  useEffect(() => {
    api.get('/users/me').then(({ data }) => {
      setProfile(data)
      setFirstName(data.firstName)
      setLastName(data.lastName)
    })
  }, [])

  const updateProfile = async (e: FormEvent) => {
    e.preventDefault()
    setMsg('')
    try {
      const { data } = await api.put('/users/me', { firstName, lastName })
      setProfile(data)
      if (token) setAuth(token, data)
      setMsg('Profile updated.')
    } catch {
      setMsg('Update failed.')
    }
  }

  const changePassword = async (e: FormEvent) => {
    e.preventDefault()
    setPwMsg('')
    try {
      await api.patch('/users/me/password', {
        currentPassword: currentPw,
        newPassword: newPw,
      })
      setPwMsg('Password changed. Please log in again.')
      setCurrentPw('')
      setNewPw('')
    } catch (err: any) {
      setPwMsg(err.response?.data?.message || 'Password change failed.')
    }
  }

  if (!profile) return <div className="spinner" />

  return (
    <div className="page">
      <h1>Profile</h1>
      <form onSubmit={updateProfile} className="admin-form">
        <h3>Personal Info</h3>
        <label>First Name</label>
        <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
        <label>Last Name</label>
        <input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
        {msg && <p className={msg.includes('failed') ? 'error-msg' : 'success-msg'}>{msg}</p>}
        <button className="btn-primary" type="submit">
          Save
        </button>
      </form>

      <form onSubmit={changePassword} className="admin-form" style={{ marginTop: '2rem' }}>
        <h3>Change Password</h3>
        <label>Current Password</label>
        <input
          type="password"
          value={currentPw}
          onChange={(e) => setCurrentPw(e.target.value)}
          required
        />
        <label>New Password</label>
        <input
          type="password"
          value={newPw}
          onChange={(e) => setNewPw(e.target.value)}
          required
          minLength={4}
        />
        {pwMsg && (
          <p className={pwMsg.includes('failed') ? 'error-msg' : 'success-msg'}>{pwMsg}</p>
        )}
        <button className="btn-primary" type="submit">
          Change Password
        </button>
      </form>
    </div>
  )
}
