import { createBrowserRouter } from 'react-router-dom'
import { RootLayout } from './RootLayout'
import { LoginPage } from '../features/auth/LoginPage'
import { RegistroPage } from '../features/auth/RegistroPage'
import { RequireAuth } from '../features/auth/RequireAuth'
import { JugadorDetailPage } from '../features/jugadores/JugadorDetailPage'
import { OfertaP2PJugadorPage } from '../features/jugadores/OfertaP2PJugadorPage'
import { CarteraPage } from '../features/cartera/CarteraPage'
import { MovimientosPage } from '../features/cartera/MovimientosPage'
import { MercadoPage } from '../features/mercado/MercadoPage'
import { P2PPage } from '../features/p2p/P2PPage'
import { AdminRoute } from '../features/admin/AdminRoute'
import { AdminJugadoresPage } from '../features/admin/AdminJugadoresPage'
import { JugadorFormPage } from '../features/admin/JugadorFormPage'
import { RecargarSaldoPage } from '../features/admin/RecargarSaldoPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <MercadoPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'registro', element: <RegistroPage /> },
      { path: 'mercado', element: <MercadoPage /> },
      { path: 'p2p', element: <P2PPage /> },
      { path: 'jugadores/:id', element: <JugadorDetailPage /> },
      { path: 'jugadores/:id/p2p', element: <OfertaP2PJugadorPage /> },
      {
        path: 'cartera',
        element: <RequireAuth />,
        children: [
          { index: true, element: <CarteraPage /> },
          { path: 'movimientos', element: <MovimientosPage /> },
        ],
      },
      {
        path: 'admin',
        element: <AdminRoute />,
        children: [
          { path: 'jugadores', element: <AdminJugadoresPage /> },
          { path: 'jugadores/nuevo', element: <JugadorFormPage /> },
          { path: 'jugadores/:id/editar', element: <JugadorFormPage /> },
          { path: 'saldo', element: <RecargarSaldoPage /> },
        ],
      },
    ],
  },
])
