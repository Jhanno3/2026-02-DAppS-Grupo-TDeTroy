import { createBrowserRouter } from 'react-router-dom'
import { RootLayout } from './RootLayout'
import { HomePage } from '../features/home/HomePage'
import { LoginPage } from '../features/auth/LoginPage'
import { RegistroPage } from '../features/auth/RegistroPage'
import { JugadoresListPage } from '../features/jugadores/JugadoresListPage'
import { JugadorDetailPage } from '../features/jugadores/JugadorDetailPage'
import { AdminRoute } from '../features/admin/AdminRoute'
import { AdminJugadoresPage } from '../features/admin/AdminJugadoresPage'
import { JugadorFormPage } from '../features/admin/JugadorFormPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'registro', element: <RegistroPage /> },
      { path: 'jugadores', element: <JugadoresListPage /> },
      { path: 'jugadores/:id', element: <JugadorDetailPage /> },
      {
        path: 'admin/jugadores',
        element: <AdminRoute />,
        children: [
          { index: true, element: <AdminJugadoresPage /> },
          { path: 'nuevo', element: <JugadorFormPage /> },
          { path: ':id/editar', element: <JugadorFormPage /> },
        ],
      },
    ],
  },
])
