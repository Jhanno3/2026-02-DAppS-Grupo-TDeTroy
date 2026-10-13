import { createBrowserRouter } from 'react-router-dom'
import { RootLayout } from './RootLayout'
import { LoginPage } from '../features/auth/LoginPage'
import { RegistroPage } from '../features/auth/RegistroPage'
import { RequireAuth } from '../features/auth/RequireAuth'
import { JugadorDetailPage } from '../features/jugadores/JugadorDetailPage'
import { CarteraPage } from '../features/cartera/CarteraPage'
import { MercadoPage } from '../features/mercado/MercadoPage'
import { AdminRoute } from '../features/admin/AdminRoute'
import { AdminJugadoresPage } from '../features/admin/AdminJugadoresPage'
import { JugadorFormPage } from '../features/admin/JugadorFormPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <MercadoPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'registro', element: <RegistroPage /> },
      { path: 'mercado', element: <MercadoPage /> },
      { path: 'jugadores/:id', element: <JugadorDetailPage /> },
      {
        path: 'cartera',
        element: <RequireAuth />,
        children: [{ index: true, element: <CarteraPage /> }],
      },
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
